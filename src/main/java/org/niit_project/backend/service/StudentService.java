package org.niit_project.backend.service;

import io.getstream.models.UpdateUsersRequest;
import io.getstream.services.framework.StreamSDKClient;
import jakarta.validation.Valid;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.enums.Colors;
import org.niit_project.backend.enums.NotificationCategory;
import org.niit_project.backend.models.StreamUser;
import org.niit_project.backend.repository.StudentRepository;
import org.niit_project.backend.utils.PhoneNumberConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;


import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private StreamSDKClient client;


    @Autowired
    private BCryptPasswordEncoder passwordEncoder;


    Optional<Student> getCompactStudent(String userId){
        return studentRepository.findById(userId);
    }

    Optional<Student> getStudentEmail(String email){
        return studentRepository.findByEmail(email);
    }

    private Counselor getFreeCounselor() throws Exception{

        var matchOperation = Aggregation.match(Criteria.where("role").is("counselor"));
//        var addFieldsOperation = Aggregation.addFields().addField("menteesCount").withValueOfExpression("{$size: '$mentees'}").build();

        // Put the more busy counselors on top
//        var sortOperation = Aggregation.sort(Sort.Direction.DESC, "menteesCount");

        var aggregation = Aggregation.newAggregation(matchOperation);

        var results = mongoTemplate.aggregate(aggregation, "admins", Counselor.class).getMappedResults();
        var counselors = new ArrayList<>(results.stream().peek(counselor -> {
            var studentMatchAggregation = Aggregation.match(Criteria.where("counselor").is(counselor.getId()));
            var mentees = mongoTemplate.aggregate(Aggregation.newAggregation(studentMatchAggregation), "students", Student.class).getMappedResults();
            counselor.setMentees(new ArrayList<>(mentees));
        }).toList());
        counselors.sort((o1, o2) -> o2.getMentees().size() - o1.getMentees().size());

        if(results.isEmpty()){
            throw new Exception("There are no counselors yet");
        }


        // To get the free-est counselor
        return counselors.get(counselors.size()-1);
    }

    public Student registerStudent(@Valid Student student) throws Exception {
        // Null checks
        if(student.getEmail() == null){
            throw new Exception("Email cannot be null");
        }
        if(student.getPhoneNumber() == null){
            throw new Exception("Phone Number cannot be null");
        }
        if(student.getFirstName() == null){
            throw new Exception("First Name cannot be null");
        }
        if(student.getLastName() == null){
           throw new Exception("Last Name cannot be null");
        }
        if(student.getPassword() == null){
           throw new Exception("Password cannot be null");
        }

        // Just to convert the phone number properly
        // To +234 format
        student.setPhoneNumber(PhoneNumberConverter.convertToFull(student.getPhoneNumber()));

        /// To make sure that you're not trying to register with an email
        /// Or phone number that already exists
        var gottenEmail = studentRepository.findByEmail(student.getEmail());
        var gottenPhoneNumber = studentRepository.findByPhoneNumber(student.getPhoneNumber());
        if(gottenEmail.isPresent() || gottenPhoneNumber.isPresent()){
            throw new Exception(gottenEmail.isPresent()? "email already exists":"phone number already exists");
        }



        var freeCounselor = getFreeCounselor();
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        student.setId(null);
        student.setCreatedAt(LocalDateTime.now());
        student.setColor(Colors.getRandomColor());
        // Assign the free-est counselor to this user
        student.setCounselor(freeCounselor.getId());

        final Student savedStudent = studentRepository.save(student);

        var notification = new Notification();
        notification.setCategory(NotificationCategory.account);
        notification.setTitle("Welcome to Scholarly");
        notification.setContent("We're really excited to have you here in scholarly 👋");
        notification.setTimestamp(LocalDateTime.now());
        notification.setRecipients(List.of(savedStudent.getId()));
        notification.setTarget(savedStudent.getId());
        // For Now because of Push Notifs not being done in the app. We'll comment this,
//        notificationService.sendPushNotification(notification);


        // Once the user has been saved with this counselor, we update it on the counselors end as well
        freeCounselor.addMentee(savedStudent.getId());
        mongoTemplate.save(freeCounselor, "admins");


        savedStudent.setToken(generateToken(savedStudent.getId()));

        // We also create a Stream Account for the user
        createStreamUser(savedStudent);

        return savedStudent;
    }

    public boolean isStudent(String studentId){
        return studentRepository.existsById(studentId);
    }

    public Student login(@Valid Student student) throws Exception{
        // Just to convert the phone number properly
        // To +234 format
        if(student.getPhoneNumber() != null){
            student.setPhoneNumber(PhoneNumberConverter.convertToFull(student.getPhoneNumber()));
        }

        final boolean isEmailLogin = student.getEmail() != null;

        if(!isEmailLogin && student.getPhoneNumber() == null){
            throw new Exception("Either Phone Number Or Email must be used");
        }

        Optional<Student> gottenStudent = isEmailLogin? studentRepository.findByEmail(student.getEmail()): studentRepository.findByPhoneNumber(student.getPhoneNumber());

        if(gottenStudent.isEmpty()){
            throw new Exception("Student not found");
        }

        if(!passwordEncoder.matches(student.getPassword(), gottenStudent.get().getPassword())){
            throw new Exception("Wrong password");
        }

        var loggedInStudent = gottenStudent.get();
        var token = generateToken(loggedInStudent.getId());
        loggedInStudent.setToken(token);

        // We generate the token return it
        return loggedInStudent;
    }

    public String generateToken(String userId) throws Exception{

        // We check if the admin/user exists before we create such token
        var userExists = studentRepository.existsById(userId);
        if(!userExists){
            throw new Exception("Student Doesn't exist");
        }


//        var env = Dotenv.load();
//        var tokenUtil = new JwtTokenUtil();
//        tokenUtil.setSecretKey(env.get("STREAM_API_SECRET"));
//        return tokenUtil.generateToken(userId);

        // 24 hour expiration
        return client.tokenBuilder().createToken(userId, 60 * 60 * 24);


//        try {
////            String token = Jwts.builder()
////                    .setSubject(userId)
////                    .setIssuedAt(new Date())
////                    .setExpiration(new Date(System.currentTimeMillis() + (24L * 60 * 60 * 1_000_000))) // 1000 day validity
////                    .signWith(SignatureAlgorithm.HS256, env.get("STREAM_API_SECRET").getBytes())
////                    .compact();


//          Option 2
//            Map<String, String> payload = new HashMap<>();
//            payload.put("user_id", userId);
//            ObjectMapper objectMapper = new ObjectMapper();
//            String jsonPayload = objectMapper.writeValueAsString(payload);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("video.stream-io-api.com/api/v2/users?api_key=" + env.get("STREAM_API_KEY")))
//                    .header("Content-Type", "application/json")
//                    .header("")
//                    .header("Authorization", "Bearer " + )
//                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
//                    .build();
//
//            // Send request
//            HttpClient client = HttpClient.newHttpClient();
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            System.out.println(response.body());
//
//            var token = new ObjectMapper().readTree(response.body()).get("token").asText();
//            return token;
//        } catch (Exception e) {
//            throw new Exception(e.getMessage());
//        }
    }

    public Optional<Student> updatePlayerId(String id, String playerId){
        var gottenAdmin = getCompactStudent(id);

        if(gottenAdmin.isEmpty()){
            return Optional.empty();
        }

        var queriedStudent = gottenAdmin.get();
        queriedStudent.setPlayerId(playerId);
        return Optional.of(studentRepository.save(queriedStudent));
    }

    public void createStreamUser(Student student) throws Exception{
        var streamUser = new StreamUser();
        streamUser.setId(student.getId());
        streamUser.setName(student.getFullName());
        streamUser.setColor(student.getColor());
        streamUser.setRole("user");


        var response = client.updateUsers(
                UpdateUsersRequest.builder()
                        .users(Map.of(student.getId(), streamUser.toUserRequest()))
                        .build()
        ).execute();


    }
}
