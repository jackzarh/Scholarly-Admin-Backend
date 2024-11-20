package org.niit_project.backend.entities;

public enum MessageType {
    // If it is a normal chat
    chat,

    // If channel name, description, details or profile photo was changed.
    update,

    // If such chat has to do with a member, probably he/she left or was added
    member,

    // If a channel was created
    create
}
