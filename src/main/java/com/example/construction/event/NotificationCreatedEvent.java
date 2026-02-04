package com.example.construction.event;

import com.example.construction.model.Notification;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationCreatedEvent extends ApplicationEvent {
    private final Notification notification;

    public NotificationCreatedEvent(Object source, Notification notification) {
        super(source);
        this.notification = notification;
    }
}
