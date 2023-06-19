package com.balumbo.blb.security;

import com.balumbo.blb.model.*;
import com.balumbo.blb.objects.DataRow;
import com.balumbo.blb.repository.*;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@EnableAsync
public class EmailService {

    static String urlPath = "http://localhost:8080";

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private MailListRepository mailListRepository;

    @Autowired
    private SequenceListRepository sequenceListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailRowRepository mailRowRepository;

    @Autowired
    private BlacklistRepository blacklistRepository;

    @PostConstruct
    public void resetOngoing(){
        System.out.println("starting up. Resetting ongoing on unfinished lists with date after current.");
        ArrayList<MailList> mailLists = mailListRepository.findAllByFinishedAndDispatchDateEqualOrBefore(false, java.sql.Date.valueOf(LocalDate.now()));
        ArrayList<SequenceList> sequenceLists = sequenceListRepository.findAllByFinished(false);
        for(int i = 0; i<mailLists.size();i++){
            mailLists.get(i).setOngoing(false);
        }
        for(int i = 0; i<sequenceLists.size();i++){
            sequenceLists.get(i).setOngoing(false);
        }
        mailListRepository.saveAll(mailLists);
        sequenceListRepository.saveAll(sequenceLists);
    }

    @Scheduled(fixedRate = 20000) // Check for changes in the mailList every 20 seconds
    public void sendEmails() {
        ArrayList<MailList> mailLists = mailListRepository.findAllByFinishedAndDispatchDateEqualOrBeforeAndOngoing(false, false, java.sql.Date.valueOf(LocalDate.now()));
        for(int i = 0; i<mailLists.size();i++){
            User user = userRepository.findById(mailLists.get(i).getUserId());
            if(!user.isError()){
                if(isWithinWorkingHours()){
                    applicationEventPublisher.publishEvent(new HandleMailListEvent(mailLists.get(i)));
                }
            }
            else{
                System.out.println("List cannot sent, user " + user.getEmail() + " has errors");
            }
        }
    }
    @Scheduled(fixedRate = 10000) // Check for changes in the mailList every 5 seconds
    public void sendSequences() {
        ArrayList<SequenceList> sequenceLists = sequenceListRepository.findAllByOngoingAndFinished(false, false);
        for(int i = 0; i<sequenceLists.size();i++){
            User user = userRepository.findById(sequenceLists.get(i).getUserId());
            if(!user.isError()){
                if(isWithinWorkingHours()){
                    applicationEventPublisher.publishEvent(new HandleSequenceEvent(sequenceLists.get(i)));
                }
            }
            else{
                System.out.println("List cannot sent, user " + user.getEmail() + " has errors");
            }
        }
    }

    @Async
    @EventListener
    public void handleMailList(HandleMailListEvent event) {
        MailList mailList = event.getMailList();
        User user = userRepository.findById(mailList.getUserId());
        ArrayList<Blacklist> blacklists = blacklistRepository.findAllByUserId(mailList.getUserId());
        ArrayList<String> blackListEmails = new ArrayList<>();
        for(int i = 0; i<blacklists.size();i++){
            blackListEmails.add(blacklists.get(i).getEmail());
        }
        ArrayList<MailRow> mailRows;
        if (blackListEmails.isEmpty()) {
            mailRows = mailRowRepository.findByMailListIdAndSentAndErrorAndIsHeaderIsFalse(mailList.getId(), false, false);
        } else {
            mailRows = mailRowRepository.findByMailListIdAndSentAndErrorAndIsHeaderIsFalseAndEmailNotIn(mailList.getId(), false, false, blackListEmails);
        }

        ArrayList<MailRow> allMailRows = mailRowRepository.findByMailListIdAndErrorIsFalseAndSentIsNull(mailList.getId());

        boolean allSentForThisSequence = false;
        if(allMailRows.size()==0) {
            allSentForThisSequence = true;
        }

        mailList.setOngoing(true);
        if(mailRows.size()==0){
            mailList.setFinished(allSentForThisSequence);
            if(allSentForThisSequence){
                mailList.setOngoing(false);
                sendFinishedList(user, mailList, false);
                System.out.println("Mail list complete. Setting finished.");
            }
        }
        mailListRepository.save(mailList);

        for(int i = 0; i<mailRows.size();i++){
            user = userRepository.findById(user.getId()).get();
            if(emailValidation(user)) {
                if(isWithinWorkingHours()){
                    try {
                        mailList = mailListRepository.findById(mailList.getId()).get();
                        sendEmail(mailRows.get(i), user, mailList);
                        mailRows.get(i).setSent(true);
                        mailRows.get(i).setSentDate(Date.valueOf(returnDateWithTime()));
                        mailRowRepository.save(mailRows.get(i));
                        Thread.sleep(mailList.getIntervalPeriod()*1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mailRows.get(i).setError(true);
                        mailRowRepository.save(mailRows.get(i));
                    }
                }
                else{
                    return;
                }
            }
            else{
                mailList.setOngoing(false);
                mailListRepository.save(mailList);
                sendErrorEmail(user);
                return;
            }
        }
        mailList.setOngoing(false);
        mailListRepository.save(mailList);
    }
    @Async
    @EventListener
    public void handleSequenceList(HandleSequenceEvent event) {
        SequenceList sequenceList = event.getSequenceList();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -sequenceList.getSequenceAfterDays()); // Subtract 10 days from the current date
        java.sql.Date dateBeforeXDays = new java.sql.Date(cal.getTimeInMillis());

        MailList mailList = mailListRepository.findById(sequenceList.getMailListId());
        User user = userRepository.findById(sequenceList.getUserId());
        ArrayList<Blacklist> blacklists = blacklistRepository.findAllByUserId(sequenceList.getUserId());
        ArrayList<String> blackListEmails = new ArrayList<>();
        for(int i = 0; i<blacklists.size();i++){
            blackListEmails.add(blacklists.get(i).getEmail());
        }
        ArrayList<MailRow> mailRows;
        if (blackListEmails.isEmpty()) {
            mailRows = mailRowRepository.findByMailListIdAndErrorAndIsHeaderIsFalseAndSentDateEqualOrAfterXDays(mailList.getId(), false, dateBeforeXDays);
        } else {
            mailRows = mailRowRepository.findByMailListIdAndErrorAndIsHeaderIsFalseAndSentDateEqualOrAfterXDaysAndEmailNotIn(mailList.getId(), false, dateBeforeXDays, blackListEmails);
        }

        sequenceList.setOngoing(true);

        ArrayList<MailRow> allMailRows = mailRowRepository.findByMailListId(mailList.getId());

        boolean allSentForThisSequence = false;
        if(allMailRows.size()>0) {
            for (int i = 0; i < allMailRows.size(); i++) {
                if (allMailRows.get(i).getSentDate() != null) {
                    if (allMailRows.get(i).getSentDate().after(dateBeforeXDays) && sequenceList.isStartedSending()) {
                        allSentForThisSequence = true;
                    }
                    else{
                        allSentForThisSequence = false;
                    }
                }
            }
        }

        if(mailRows.size()==0){
            System.out.println("no emails to send to, all are too early");
            sequenceList.setOngoing(false);
            sequenceList.setFinished(allSentForThisSequence);
        }
        sequenceListRepository.save(sequenceList);

        for(int i = 0; i<mailRows.size();i++){
            user = userRepository.findById(user.getId()).get();
            if(emailValidation(user)) {
                if(isWithinWorkingHours()){
                    //Lägg till en if sats som kollar om användaren har svarat inloggade personen
                    try {
                        sequenceList = sequenceListRepository.findById(sequenceList.getId()).get();
                        sendSequenceEmail(mailRows.get(i), user, sequenceList, mailList);
                        mailRows.get(i).setSentDate(Date.valueOf(returnDateWithTime()));
                        mailRowRepository.save(mailRows.get(i));
                        sequenceList.setStartedSending(true);
                        sequenceListRepository.save(sequenceList);
                        Thread.sleep(mailList.getIntervalPeriod()*1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mailRows.get(i).setError(true);
                        mailRowRepository.save(mailRows.get(i));
                    }
                }
                else{
                    return;
                }
            }
            else{
                sequenceList.setOngoing(false);
                sequenceListRepository.save(sequenceList);
                sendErrorEmail(user);
                return;
            }
        }
        sequenceList.setOngoing(false);
        sequenceListRepository.save(sequenceList);
    }
    public String returnDateWithTime(){
        java.util.Date date = new java.util.Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(TimeZone.getTimeZone("Europe/Stockholm"));
        return df.format(date);
    }
    public void sendErrorEmail(User user){
        user.setError(true);
        userRepository.save(user);
        try{
            // Recipient's email ID needs to be mentioned.
            String to = user.getEmail();

            // Sender's email ID needs to be mentioned
            String from = "jesper@ensotech.io";
            final String username = user.getMailEmail();
            final String password = user.getMailPassword();

            // Assuming you are sending email through relay.jangosmtp.net
            String host = user.getMailHost();

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "mailcluster.loopia.se");
            props.put("mail.smtp.port", "587");

            // Get the Session object.
            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication("jesper@ensotech.io", "!Jesper1337");
                        }
                    });
            // Create a default MimeMessage object.
            Message message = new MimeMessage(session);
            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from, user.getMailAlias()));
            // Set To: header field of the header.
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to));

            //Replace the title with variables
            Pageable pageableHeader = PageRequest.of(0, 1);

            String replacedTitle = "[VIKTIGT BALUMBO] Epost iställningar felaktiga";

            // Set Subject: header field
            message.setSubject(replacedTitle);

            //Replace the content with variables
            String replacedContent = "Epost uppgifterna måste uppdateras i inställningar";

            // Now set the actual message
            message.setContent(replacedContent, "text/html; charset=UTF-8");
            // Send message
            Transport.send(message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void sendFinishedList(User user, MailList mailList, boolean isSequence){
        try{
            // Recipient's email ID needs to be mentioned.
            String to = user.getEmail();

            // Sender's email ID needs to be mentioned
            String from = user.getMailEmail();
            final String username = user.getMailEmail();
            final String password = user.getMailPassword();

            // Assuming you are sending email through relay.jangosmtp.net
            String host = user.getMailHost();

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", user.getMailPort());

            // Get the Session object.
            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });
            // Create a default MimeMessage object.
            Message message = new MimeMessage(session);
            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from, user.getMailAlias()));
            // Set To: header field of the header.
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to));

            //Replace the title with variables
            Pageable pageableHeader = PageRequest.of(0, 1);

            String replacedTitle = "Ditt utskick är klart.";

            // Set Subject: header field
            message.setSubject(replacedTitle);
            String replacedContent;
            if(!isSequence){
                //Replace the content with variables
                replacedContent = "Utskicket '" + mailList.getFileName() + "' har slutförts.";
            }
            else{
                //Replace the content with variables
                replacedContent = "Sekvens för utskicket '" + mailList.getFileName() + "' har slutförts.";
            }

            // Now set the actual message
            message.setContent(replacedContent, "text/html; charset=UTF-8");
            // Send message
            Transport.send(message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public boolean isWithinWorkingHours() {
        ZonedDateTime now = ZonedDateTime.now();
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        if (day.equals(DayOfWeek.SATURDAY) || day.equals(DayOfWeek.SUNDAY)) {
            return false;  // It's a weekend
        }

        LocalTime startOfWorkDay = LocalTime.of(8, 0);
        //På heroku är tidszonen 2h bakåt, så den slutar skicka vid 17 (sommartid)
        LocalTime endOfWorkDay = LocalTime.of(15, 0);
        return !time.isBefore(startOfWorkDay) && !time.isAfter(endOfWorkDay);
    }
    public boolean emailValidation(User user) {
        final String username = user.getMailEmail();
        final String password = user.getMailPassword();

        // Assuming you are sending email through relay.jangosmtp.net
        String host = user.getMailHost();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", user.getMailPort());

        // Get the Session object.
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
        try {
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
    public void sendEmail(MailRow mailRow, User user, MailList mailList) throws MessagingException, IOException, CsvException {
        // Recipient's email ID needs to be mentioned.
        String to = mailRow.getEmail();

        // Sender's email ID needs to be mentioned
        String from = user.getMailEmail();
        final String username = user.getMailEmail();
        final String password = user.getMailPassword();

        // Assuming you are sending email through relay.jangosmtp.net
        String host = user.getMailHost();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", user.getMailPort());

        // Get the Session object.
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
        // Create a default MimeMessage object.
        Message message = new MimeMessage(session);
        // Set From: header field of the header.
        message.setFrom(new InternetAddress(from, user.getMailAlias()));
        // Set To: header field of the header.
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(to));

        //Replace the title with variables
        Pageable pageableHeader = PageRequest.of(0, 1);

        String replacedTitle = mailList.getTitle();
        Page<MailRow> headerValues = mailRowRepository.findByMailListIdAndIsHeader(mailList.getId(), true, pageableHeader);
        ArrayList<DataRow> variables = parseCSVRows(headerValues.getContent().get(0).getDataRow(), 0, mailList.getSeparatorValue(), true);

        //Make a complete line

        ArrayList<DataRow> values = parseCSVRows(mailRow.getDataRow(), 0, mailList.getSeparatorValue(), false);

        for(int i = 0; i<variables.size();i++){
            replacedTitle = replacedTitle.replaceAll("\\{" + variables.get(i).getName() + "}", values.get(variables.get(i).getIndex()).getName());
        }
        // Set Subject: header field
        message.setSubject(replacedTitle);

        //Replace the content with variables
        String replacedContent = mailList.getMainContent();

        String replacedFooter = mailList.getFooterContent();

        for(int i = 0; i<variables.size();i++){
            replacedContent = replacedContent.replaceAll("\\{" + variables.get(i).getName() + "}", values.get(variables.get(i).getIndex()).getName());
        }

        for(int i = 0; i<variables.size();i++){
            replacedFooter = replacedFooter.replaceAll("\\{" + variables.get(i).getName() + "}", values.get(variables.get(i).getIndex()).getName());
        }
        String openedLink = "<img src=\""+ urlPath +"/track/" + mailRow.getId() + "\"width=\"1\" height=\"1\" border=\"0\" />";
        replacedContent = replacedContent.replaceAll("\n", "<br>");
        replacedFooter = replacedFooter.replaceAll("\n", "<br>");

        // Now set the actual message
        message.setContent(replacedContent + "<br><br>" + replacedFooter + " " +  openedLink, "text/html; charset=UTF-8");
        // Send message
        Transport.send(message);
        System.out.println("Sent message successfully for user " + user.getEmail() + ". Interval=" + mailList.getIntervalPeriod() + "s");
    }
    public void sendSequenceEmail(MailRow mailRow, User user, SequenceList sequenceList, MailList mailList) throws MessagingException, IOException, CsvException {
        // Recipient's email ID needs to be mentioned.
        String to = mailRow.getEmail();

        // Sender's email ID needs to be mentioned
        String from = user.getMailEmail();
        final String username = user.getMailEmail();
        final String password = user.getMailPassword();

        // Assuming you are sending email through relay.jangosmtp.net
        String host = user.getMailHost();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", user.getMailPort());

        // Get the Session object.
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
        // Create a default MimeMessage object.
        Message message = new MimeMessage(session);
        // Set From: header field of the header.
        message.setFrom(new InternetAddress(from, user.getMailAlias()));
        // Set To: header field of the header.
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(to));

        //Replace the title with variables
        Pageable pageableHeader = PageRequest.of(0, 1);

        String replacedTitle = sequenceList.getTitle();
        Page<MailRow> headerValues = mailRowRepository.findByMailListIdAndIsHeader(mailList.getId(), true, pageableHeader);
        ArrayList<DataRow> variables = parseCSVRows(headerValues.getContent().get(0).getDataRow(), 0, mailList.getSeparatorValue(), true);

        //Make a complete line

        ArrayList<DataRow> values = parseCSVRows(mailRow.getDataRow(), 0, mailList.getSeparatorValue(), false);

        for(int i = 0; i<variables.size();i++){
            replacedTitle = replacedTitle.replaceAll("\\{" + variables.get(i).getName() + "}", values.get(variables.get(i).getIndex()).getName());
        }
        // Set Subject: header field
        message.setSubject(replacedTitle);

        //Replace the content with variables
        String replacedContent = sequenceList.getMainContent();

        String replacedFooter = mailList.getFooterContent();

        for(int i = 0; i<variables.size();i++){
            replacedContent = replacedContent.replaceAll("\\{" + variables.get(i).getName() + "}", values.get(variables.get(i).getIndex()).getName());
        }

        for(int i = 0; i<variables.size();i++){
            replacedFooter = replacedFooter.replaceAll("\\{" + variables.get(i).getName() + "}", values.get(variables.get(i).getIndex()).getName());
        }
        String openedLink = "<img src=\""+ urlPath +"/track/" + mailRow.getId() + "\"width=\"1\" height=\"1\" border=\"0\" />";
        replacedContent = replacedContent.replaceAll("\n", "<br>");
        replacedFooter = replacedFooter.replaceAll("\n", "<br>");

        // Now set the actual message
        message.setContent(replacedContent + "<br><br>" + replacedFooter + " " +  openedLink, "text/html; charset=UTF-8");
        // Send message
        Transport.send(message);
        System.out.println("Sent message successfully for user " + user.getEmail() + ". Interval=" + mailList.getIntervalPeriod() + "s");
    }
    public ArrayList<DataRow> parseCSVRows(String completeData, int skipLines, String separator, boolean isHeader) throws IOException, CsvException {
        CSVParser csvParser = new CSVParserBuilder().withSeparator(separator.charAt(0)).build(); // custom separator
        CSVReader reader = new CSVReaderBuilder(
                new StringReader(completeData))
                .withCSVParser(csvParser)
                .withSkipLines(skipLines)
                .build();
        String[] firstLine = reader.readNext();
        ArrayList<DataRow> dataRowsList = new ArrayList<>();
        for(int i = 0; i<firstLine.length;i++){
            DataRow dataRow = new DataRow();
            if(isHeader){
                dataRow.setName(firstLine[i].replaceAll(" ", "").toLowerCase());
            }
            else{
                dataRow.setName(firstLine[i]);
            }
            dataRow.setIndex(i);
            dataRowsList.add(dataRow);
        }
        return dataRowsList;
    }

}
