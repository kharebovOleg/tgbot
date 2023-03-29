package ru.kharebov.tgBot.tgbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import ru.kharebov.tgBot.tgbot.dto.GetCursOnDateXml;
import ru.kharebov.tgBot.tgbot.dto.GetCursOnDateXmlResponse;
import ru.kharebov.tgBot.tgbot.dto.ValuteCursOnDate;


import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

//Данный класс наследуется от WebServiceTemplate, который предоставляет удобный способ взаимодействия с SOAP веб сервисами
public class CentralRussianBankService extends WebServiceTemplate {
    //Тут случается некоторая магия Spring и в момент запуска вашего приложения, сюда поставляется значение из application.properties или application.yml
    @Value("${cbr.api.url}")
    private String cbrApiUrl;

    //Создаем метод получения данных
    public List<ValuteCursOnDate> getCurrenciesFromCbr() throws DatatypeConfigurationException {
        //1. формируем объект GetCursOnDateXml с текущей датой
        final GetCursOnDateXml getCursOnDateXML = new GetCursOnDateXml();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        XMLGregorianCalendar xmlGregCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        getCursOnDateXML.setOnDate(xmlGregCal);

        //2. marshalSendAndReceive отправляет на cbrApiUrl маршализованный getCursOnDateXML и возвращает
        //демаршализованный response
        GetCursOnDateXmlResponse response = (GetCursOnDateXmlResponse) marshalSendAndReceive(cbrApiUrl, getCursOnDateXML);

        //3. отрабатываем возможную ошибку
        if (response == null) {
            throw new IllegalStateException("Could not get response from CBR Service");
        }

        //4. при помощи метода trim() удаляем лишние пробелы
        final List<ValuteCursOnDate> courses = response.getGetCursOnDateXmlResult().getValuteData();
        courses.forEach(course -> course.setName(course.getName().trim()));
        return courses;
    }
}
