package org.niit_project.backend.config;

import org.niit_project.backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class WebSocketEventListener {

    @Autowired
    private AdminService adminService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event){

    }

    @EventListener
    public void handleWebSocketSubscriptionListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        String subscriptionId = headerAccessor.getSubscriptionId();
        // The broker destination being subscribed to

        assert destination != null;
        if(destination.startsWith("/admins") || destination.contains("admins")){
            handleAdminSubscription(subscriptionId, false);
            return;
        }


    }
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        String subscriptionId = headerAccessor.getSubscriptionId();

        assert destination != null;
        if(destination.startsWith("/admins") || destination.contains("admins")){
            handleAdminSubscription(subscriptionId, true);
            return;
        }

//        System.out.println("Unsubscribed: Session ID = " + sessionId + ", Subscription ID = " + subscriptionId);
    }


    void handleAdminSubscription(String adminId, boolean disconnect){
        var foundAdmin = adminService.getAdmin(adminId);

        if(foundAdmin.isEmpty()){
            return;
        }

        var admin = foundAdmin.get();
        admin.setOnline(!disconnect);

        adminService.updateAdmin(adminId, admin);
    }

}
