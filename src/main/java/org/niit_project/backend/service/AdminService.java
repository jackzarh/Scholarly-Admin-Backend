package org.niit_project.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.validation.Valid;
import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {
    @Autowired
    private AdminRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;


    public Admin login(Admin admin) throws Exception {
        Optional<Admin> gottenAdmin;

        final boolean isEmailLogin = admin.getEmail() != null;

        if(!isEmailLogin && admin.getPhoneNumber() == null){
            throw new Exception("Either Phone Number Or Email must be used");
        }

        gottenAdmin = isEmailLogin? userRepository.findByEmail(admin.getEmail()): userRepository.findByPhoneNumber(admin.getPhoneNumber());

        if(gottenAdmin.isEmpty()){
            throw new Exception("Admin not found");
        }

        if(!passwordEncoder.matches(admin.getPassword(), gottenAdmin.get().getPassword())){
            throw new Exception("Invalid Credentials");
        }

        var loggedInAdmin = gottenAdmin.get();
        loggedInAdmin.setToken(generateToken(admin.getId()));

        // We generate the token return it
        return loggedInAdmin;



    }

    public Optional<Admin> getAdmin(String id){
        return userRepository.findById(id);
    }

    public Optional<Admin> getAdminByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public Optional<List<Admin>> getAllAdmins(){
        try{
            var allAdmins = userRepository.findAll();

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
        try{

            final Admin savedAdmin = userRepository.save(admin);
            savedAdmin.setToken(generateToken(savedAdmin.getId()));
            return Optional.of(savedAdmin);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Admin> updateAdmin(String id, Admin admin){
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


        return Optional.of(userRepository.save(admin));
    }

    public Optional<Admin> updateAdminProfile(String id, String url){
        /// Only the url is edited.


        var gottenAdmin = getAdmin(id);

        if(gottenAdmin.isEmpty()){
            return Optional.empty();
        }

        var queriedAdmin = gottenAdmin.get();

        queriedAdmin.setProfile(url);


        return Optional.of(userRepository.save(queriedAdmin));
    }

    public String generateToken(String userId) throws Exception{
        var env = Dotenv.load();
        try{
            String token = Jwts.builder()
                    .setSubject(userId)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000))) // 1 day validity
                    .signWith(SignatureAlgorithm.HS256, env.get("STREAM_API_SECRET").getBytes())
                    .compact();


            /// Option 2
//            Map<String, String> payload = new HashMap<>();
//            payload.put("user_id", userId);
//            ObjectMapper objectMapper = new ObjectMapper();
//            String jsonPayload = objectMapper.writeValueAsString(payload);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("https://chat.stream-io-api.com/v1.0/chat/token"))
//                    .header("Content-Type", "application/json")
//                    .header("Authorization", "Bearer " + env.get("STREAM_API_KEY"))
//                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
//                    .build();
//
//            // Send request
//            HttpResponse<String> response;
//            try (HttpClient client = HttpClient.newHttpClient()) {
//                response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            }
//
//            var token = new ObjectMapper().readTree(response.body()).get("token").asText();
            return token;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }




}
