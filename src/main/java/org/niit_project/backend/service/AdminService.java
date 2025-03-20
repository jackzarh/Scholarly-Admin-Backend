package org.niit_project.backend.service;


//import im.zego.serverassistant.sample.Token04SampleBase;
import io.getstream.models.UpdateUsersRequest;
import io.getstream.services.framework.StreamSDKClient;
import jakarta.validation.Valid;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

@Service
public class AdminService {
    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private StreamSDKClient client;


    public Admin login(Admin admin) throws Exception {
        Optional<Admin> gottenAdmin;

        final boolean isEmailLogin = admin.getEmail() != null;

        if(!isEmailLogin && admin.getPhoneNumber() == null){
            throw new Exception("Either Phone Number Or Email must be used");
        }

        gottenAdmin = isEmailLogin? adminRepository.findByEmail(admin.getEmail()): adminRepository.findByPhoneNumber(admin.getPhoneNumber());

        if(gottenAdmin.isEmpty()){
            throw new Exception("Admin not found");
        }

        if(!passwordEncoder.matches(admin.getPassword(), gottenAdmin.get().getPassword())){
            throw new Exception("Invalid Credentials");
        }

        var loggedInAdmin = gottenAdmin.get();
        var token = generateToken(loggedInAdmin.getId());
        loggedInAdmin.setToken(token);

        // We generate the token return it
        return loggedInAdmin;



    }

    public Optional<Admin> getAdmin(String id){
        return adminRepository.findById(id);
    }

    public Optional<Admin> getAdminByEmail(String email){
        return adminRepository.findByEmail(email);
    }

    public Optional<List<Admin>> getAllAdmins(){
        try{
            var allAdmins = adminRepository.findAll();

            // We sort the admins in order of their created date
            allAdmins.sort((o1, o2) -> {
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            });
            return Optional.of(allAdmins);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Admin> registerUser(@Valid Admin admin) {
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setId(null);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setColor(Colors.getRandomColor());
        try{
            final Admin savedAdmin = adminRepository.save(admin);
            savedAdmin.setToken(generateToken(savedAdmin.getId()));

            // We also create a Stream Account for the user
            createStreamUser(savedAdmin);

            var notification = new Notification();
            notification.setCategory(NotificationCategory.account);
            notification.setTitle("Welcome to Scholarly");
            notification.setContent("We're really excited to have you here in scholarly, " + savedAdmin.getFirstName());
            notification.setTimestamp(LocalDateTime.now());
            notification.setRecipients(List.of(savedAdmin.getId()));
            notification.setTarget(savedAdmin.getId());
            notificationService.sendPushNotification(notification);

            return Optional.of(savedAdmin);

        }
        catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Used to either create or update a User
     * @param admin
     * @throws Exception
     */
    public void createStreamUser(Admin admin) throws Exception{
        var streamUser = new StreamUser();
        streamUser.setId(admin.getId());
        streamUser.setName(admin.getFullName());
        streamUser.setColor(admin.getColor());
        streamUser.setRole("admin");

        var response = client.updateUsers(
                UpdateUsersRequest.builder()
                        .users(Map.of(admin.getId(), streamUser.toUserRequest()))
                        .build()
        ).execute();

    }

    public boolean isAdmin(String adminId){
        return adminRepository.existsById(adminId);
    }

    public Optional<Admin> updateAdmin(String id, Admin admin) {
        /// Only the firstName, lastName are edited.
        var gottenAdmin = getAdmin(id);

        if(gottenAdmin.isEmpty()){
            return Optional.empty();
        }

        var queriedAdmin = gottenAdmin.get();


        admin.setId(id);
        admin.setEmail(queriedAdmin.getEmail());
        admin.setPhoneNumber(queriedAdmin.getPhoneNumber());
        admin.setCreatedAt(queriedAdmin.getCreatedAt());
        admin.setPassword(queriedAdmin.getPassword());
        admin.setRole(queriedAdmin.getRole());
        admin.setProfile(queriedAdmin.getProfile());

        var savedAdmin = adminRepository.save(admin);
        try {
            createStreamUser(admin);
        } catch (Exception e) {
            return Optional.empty();
        }


        return Optional.of(savedAdmin);
    }

    public Optional<Admin> updatePlayerId(String id, String playerId){
        /// Only the firstName, lastName are edited.
        var gottenAdmin = getAdmin(id);

        if(gottenAdmin.isEmpty()){
            return Optional.empty();
        }

        var queriedAdmin = gottenAdmin.get();
        queriedAdmin.setPlayerId(playerId);
        return Optional.of(adminRepository.save(queriedAdmin));
    }

    public Optional<Admin> updateAdminProfile(String id, String url){
        /// Only the url is edited.


        var gottenAdmin = getAdmin(id);

        if(gottenAdmin.isEmpty()){
            return Optional.empty();
        }

        var queriedAdmin = gottenAdmin.get();

        queriedAdmin.setProfile(url);


        return Optional.of(adminRepository.save(queriedAdmin));
    }

    public String generateToken(String userId) throws Exception{

        // We check if the admin/user exists before we create such token
        var userExists = adminRepository.existsById(userId);
        if(!userExists){
            throw new Exception("Admin Doesn't exist");
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




}
