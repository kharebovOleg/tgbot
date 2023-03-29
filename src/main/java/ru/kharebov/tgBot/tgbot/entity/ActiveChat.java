package ru.kharebov.tgBot.tgbot.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "active_chat")
public class ActiveChat {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id")
    private Long chatId;
}
