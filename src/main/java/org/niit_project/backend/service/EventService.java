package org.niit_project.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.niit_project.backend.entities.Event;
import org.niit_project.backend.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    public Event createEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null.");
        }
        event.setCreatedTime(LocalDateTime.now());
        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> getEventById(String id) {
        return eventRepository.findById(id);
    }

    public List<Event> searchEvents(String keyword) {
        return eventRepository.findByEventTitleContainingIgnoreCaseOrEventDescriptionContainingIgnoreCase(keyword, keyword);
    }

    public List<Object> getCompactEvents() {
        return eventRepository.findAll()
                .stream()
                .map(event -> {
                    return Map.of(
                            "eventTitle", event.getEventTitle(),
                            "keyInformation", event.getKeyInformation(),
                            "createdTime", event.getCreatedTime(),
                            "designatedTime", event.getDesignatedTime()
                    );
                })
                .collect(Collectors.toList());
    }

    public List<Event> getEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return eventRepository.findByDesignatedTimeBetween(startDate, endDate);
    }

    public void deleteEvent(String id) {
        eventRepository.deleteById(id);
    }

    public Optional<Event> updateEvent(String id, Event updatedEvent) {
        Optional<Event> existingEvent = eventRepository.findById(id);
        if (existingEvent.isPresent()) {
            Event event = existingEvent.get();

            if (updatedEvent.getEventTitle() != null) {
                event.setEventTitle(updatedEvent.getEventTitle());
            }
            if (updatedEvent.getEventDescription() != null) {
                event.setEventDescription(updatedEvent.getEventDescription());
            }
            if (updatedEvent.getAudience() != null) {
                event.setAudience(updatedEvent.getAudience());
            }
            if (updatedEvent.getKeyInformation() != null) {
                event.setKeyInformation(updatedEvent.getKeyInformation());
            }
            if (updatedEvent.getDesignatedTime() != null) {
                event.setDesignatedTime(updatedEvent.getDesignatedTime());
            }
            return Optional.of(eventRepository.save(event));
        }
        return Optional.empty();
    }

    public Optional<Event> updateEventPhoto(String id, MultipartFile file) {
        Optional<Event> eventOptional = eventRepository.findById(id);
        if (eventOptional.isPresent()) {
            try {
                Dotenv dotenv = Dotenv.load();
                Cloudinary cloudinary = new Cloudinary(dotenv.get("CLOUDINARY_URL"));
                var params = ObjectUtils.asMap(
                        "use_filename", true,
                        "unique_filename", false,
                        "overwrite", true
                );
                var result = cloudinary.uploader().upload(file.getBytes(), params);
                String secureUrl = result.get("secure_url").toString().trim();

                Event event = eventOptional.get();
                event.setEventPhoto(secureUrl);
                return Optional.of(eventRepository.save(event));
            } catch (IOException e) {
                throw new RuntimeException("Error uploading photo: " + e.getMessage(), e);
            }
        }
        return Optional.empty();
    }

    public long countEvents() {
        return eventRepository.count();
    }
}
