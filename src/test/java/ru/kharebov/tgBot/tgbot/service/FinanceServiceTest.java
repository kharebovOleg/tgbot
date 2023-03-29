package ru.kharebov.tgBot.tgbot.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import ru.kharebov.tgBot.tgbot.repository.IncomeRepository;
import ru.kharebov.tgBot.tgbot.repository.SpendRepository;


@SpringBootTest
// Указываем, что инстанс теста создаётся на весь класс (т.е. для отработки всех методов)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinanceServiceTest {

    // упоминание нашего сервиса с аннотацией, которая имитирует сервис
    // и включает имитацию нужных зависимостей,
    // так как в тестах не вся программа инициализируется, а только её часть
    @InjectMocks
    private FinanceService financeService;
    // указываем, что этот класс надо имитировать (он используется в FinanceService)
    @Mock
    private SpendRepository spendRepository;

    // указываем, что этот класс надо имитировать (он используется в FinanceService)
    @Mock
    private IncomeRepository incomeRepository;

    // тестовый метод, помечаем его аннотацией @Test

    @Test
    void addFinanceOperation() {
// установили произвольное значение переменной для отправки в метод
        String price = "150.0";
        // обращаемся к методу с произвольными параметрами и сохраняем результат в переменную
        String message = financeService.addFinanceOperation("/addincome", price, 500L);
        // убеждаемся, что получили ожидаемый результат
        Assertions.assertEquals("Доход в размере " + price + " был успешно добавлен", message);
        // меняем значение на какое-нибудь произвольное другое
        price = "200";
        // снова обращаемся к методу с другими параметрами
        message = financeService.addFinanceOperation("/nan", price, 250L);
        // убеждаемся, что получили ожидаемый результат
        Assertions.assertEquals("Расход в размере " + price + " был успешно добавлен", message);

    }
}