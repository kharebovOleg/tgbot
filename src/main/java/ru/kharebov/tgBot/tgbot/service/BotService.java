package ru.kharebov.tgBot.tgbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.kharebov.tgBot.tgbot.dto.ValuteCursOnDate;
import ru.kharebov.tgBot.tgbot.entity.ActiveChat;
import ru.kharebov.tgBot.tgbot.repository.ActiveChatRepository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService extends TelegramLongPollingBot {

    private static final String CURRENT_RATES = "/currentrates";
    private static final String ADD_INCOME = "/addincome";
    private static final String ADD_SPEND = "/addspend";

    private final CentralRussianBankService centralRussianBankService;
    private final FinanceService financeService;
    private final ActiveChatRepository activeChatRepository;

    @Value("${bot.api.key}")
    private String apiKey;

    @Value("${bot.name}")
    private String name;
    //чтобы где-то хранить предыдущее собщение от пользователя, чтобы знать точно,
    // на какую именно команду пользователь отправляет сумму создаем Map
    private Map<Long, List<String>> previousCommands = new ConcurrentHashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage(); //Этой строчкой мы получаем сообщение от пользователя
        try {
            //Создаем ответное сообщение(пока не отправляем, только создаем)
            SendMessage response = new SendMessage(); //Данный класс представляет собой реализацию команды отправки сообщения,
            // которую за нас выполнит ранее подключенная библиотека
            Long chatId = message.getChatId(); //ID чата, в который необходимо отправить ответ
            response.setChatId(String.valueOf(chatId)); //Устанавливаем ID, полученный из предыдущего этапа сюда,
            // чтобы сообщить, в какой чат необходимо отправить сообщение


            if (CURRENT_RATES.equalsIgnoreCase(message.getText())) {
                //Получаем все курсы валют на текущий момент и проходимся по ним в цикле
                for (ValuteCursOnDate valuteCursOnDate : centralRussianBankService.getCurrenciesFromCbr()) {
                    //В данной строчке мы собираем наше текстовое сообщение
                //StringUtils.defaultBlank – это метод из библиотеки Apache Commons, который нам нужен для того,
                // чтобы на первой итерации нашего цикла была вставлена пустая строка вместо null,
                // а на следующих итерациях не перетерся текст, полученный из предыдущих итерации.
                    response.setText(StringUtils.defaultIfBlank(response.getText(), "")
                            + valuteCursOnDate.getName() + " - " + valuteCursOnDate.getCourse() + "\n");
                }
            } else if (ADD_INCOME.equalsIgnoreCase(message.getText())) {
                response.setText("Отправьте мне сумму полученного дохода");
            } else if (ADD_SPEND.equalsIgnoreCase(message.getText())) {
                response.setText("Отправьте мне сумму расходов");
            } else {
                response.setText(financeService
                        .addFinanceOperation(getPreviousCommand(message.getChatId()),
                                message.getText(), message.getChatId()));
            }

            putPreviousCommand(message.getChatId(), message.getText());

            //Теперь мы сообщаем, что пора бы и ответ отправлять
            execute(response);
            //Проверяем, есть ли у нас такой chatId в базе, если нет, то добавляем, если есть,
            // то пропускаем данный шаг
            if (activeChatRepository.findActiveChatByChatId(chatId).isEmpty()) {
                ActiveChat activeChat = new ActiveChat();
                activeChat.setChatId(chatId);
                activeChatRepository.save(activeChat);
            }
        } catch (Exception e) {
            log.error("Возникла неизвестная проблема, сообщите пожалуйста администратору", e);
        }
    }

    //Доработаем наш BotService для отправления данных множественному количеству людей:
    public void sendNotificationToAllActiveChats(String message, Set<Long> chatIds) {
        for (Long id : chatIds) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(id));
            sendMessage.setText(message);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Не удалось отправить сообщение", e);
            }
        }
    }

    @PostConstruct // //Данный метод будет вызван сразу после того, как данный бин будет создан
    public void start() {
        log.info("username: {}, token: {}", name, apiKey);
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return apiKey;
    }

    private void putPreviousCommand(Long chatId, String command) {
        if (previousCommands.get(chatId) == null) {
            List<String> commands = new ArrayList<>();
            commands.add(command);
            previousCommands.put(chatId, commands);
        } else {
            previousCommands.get(chatId).add(command);
        }
    }

    private String getPreviousCommand(Long chatId) {
        return previousCommands.get(chatId)
                .get(previousCommands
                        .get(chatId).size() - 1);
    }
}