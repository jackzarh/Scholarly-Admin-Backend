package org.niit_project.backend.service;

import jakarta.validation.Valid;
import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        return gottenAdmin.get();



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




}
