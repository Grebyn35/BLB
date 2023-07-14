package com.balumbo.blb.security;

import com.balumbo.blb.model.*;
import com.balumbo.blb.objects.DataRow;
import com.balumbo.blb.repository.*;
import com.google.gson.*;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import javax.mail.search.FromStringTerm;
import javax.security.auth.login.LoginException;
import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EmailService {

    static String urlPath = "https://balumbo.herokuapp.com";

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

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private BatchUpdaterRepository batchUpdaterRepository;


    //Handles all the main background events
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

        //This checks if the object 'BatchUpdater' is existing, if its not it creates one
        ArrayList<BatchUpdater> batchUpdaters = batchUpdaterRepository.findAll();
        if(batchUpdaters.size()==0){
            System.out.println("created new BatchUpdater...");
            BatchUpdater batchUpdater = new BatchUpdater();
            batchUpdaterRepository.save(batchUpdater);
        }
        else{
            batchUpdaters.get(0).setUpdating(false);
            batchUpdaterRepository.save(batchUpdaters.get(0));
        }
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
    @Scheduled(fixedRate = 60000 * 1)
    public void updateCompanyBatchList() throws LoginException, IOException, InterruptedException {
        BatchUpdater batchUpdater = batchUpdaterRepository.findAll().get(0);
        //This is if there hasn't been any previous calls for updating the companies database
        if(batchUpdater.getDateUpdated()==null){
            constructCompaniesFromJson();
        }
        else{
            Date dateUpdated = Date.valueOf(batchUpdater.getDateUpdated().toString()); // Your java.sql.Date here.
            long daysAfter = 30; // The number of days to check.
            Date currentDate = Date.valueOf(LocalDate.now());

            //Check if the last update for companies was more than 'daysAfter' ago
            if (ChronoUnit.DAYS.between(dateUpdated.toLocalDate(), currentDate.toLocalDate()) >= daysAfter) {
                if(!batchUpdater.isUpdating()){
                    batchUpdater.setUpdating(true);
                    batchUpdaterRepository.save(batchUpdater);
                    constructCompaniesFromJson();
                }
                else{
                    System.out.println("company batch is already updating...");
                }
            } else {
                System.out.println("BatchUpdater date not reached yet... next update=" + dateUpdated.toLocalDate().plus(daysAfter, ChronoUnit.DAYS));
            }
        }
    }
    @Scheduled(fixedRate = 60000)
    public void updateInfo() throws InterruptedException {
        //Update companies older than 100 days
        LocalDate daysAgo = LocalDate.now().minusDays(100);
        ArrayList<Company> companies = companyRepository.findCompaniesOlderThanForInfo(Date.valueOf(daysAgo), PageRequest.of(0, 20));
        for(int i = 0; i<companies.size();i++){
            System.out.println("fetching info for " + companies.get(i).getOrgNo() + "...");
            getCmpInfo(companies.get(i));
            Thread.sleep(500);
        }
    }
    @Scheduled(fixedRate = 60000)
    public void updateWebsite() throws InterruptedException, IOException, LoginException {
        //Update companies older than 100 days
        LocalDate daysAgo = LocalDate.now().minusDays(200);
        ArrayList<Company> companies = companyRepository.findCompaniesOlderThanForWebsite(Date.valueOf(daysAgo), PageRequest.of(0, 12));
        for(int i = 0; i<companies.size();i++){
            System.out.println("fetching website for " + companies.get(i).getOrgNo() + "...");
            getCmpWebsiteEmail(companies.get(i));
            Thread.sleep(5000);
        }
    }
    //

    //Handles all the mailing functions
    public static String extractEmail(Address[] fromAddresses){
        if (fromAddresses != null && fromAddresses.length > 0) {
            if (fromAddresses[0] instanceof InternetAddress) {
                InternetAddress internetAddress = (InternetAddress) fromAddresses[0];
                return internetAddress.getAddress();
            }
        }
        return null;
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
                        //Kollar om man stäng av den så stängs threaden
                        if(!mailList.isOngoing()){
                            System.out.println("thread cancelled. shutting down running thread.");
                            return;
                        }
                        if(!mailRowRepository.findById(mailRows.get(i).getId()).get().isSent()){
                            sendEmail(mailRows.get(i), user, mailList);
                        }
                        else{
                            continue;
                        }
                        mailRows.get(i).setSent(true);
                        mailRows.get(i).setSentDate(Date.valueOf(returnDateWithTime()));
                        mailRowRepository.save(mailRows.get(i));
                        Thread.sleep(mailList.getIntervalPeriod()*1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mailRows.get(i).setError(true);
                        mailRows.get(i).setSentDate(Date.valueOf(returnDateWithTime()));
                        mailRowRepository.save(mailRows.get(i));
                    }
                }
                else{
                    mailList.setOngoing(false);
                    mailListRepository.save(mailList);
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
        if(!mailList.isFinished()){
            return;
        }
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

        ArrayList<MailRow> allMailRows = mailRowRepository.findAllByMailListIdAndErrorAndSentDateIsNotNull(mailList.getId(), false);

        boolean finished = true;
        for (int i = 0; i < allMailRows.size(); i++) {
            if (allMailRows.get(i).getSentDate().before(dateBeforeXDays) && sequenceList.isStartedSending()) {
                finished = false;
                break;
            }
        }

        sequenceList.setFinished(finished);
        if(mailRows.size()==0){
            System.out.println("no emails to send to, all are too early");
            sequenceList.setOngoing(false);
        }
        sequenceList = sequenceListRepository.save(sequenceList);

        for(int i = 0; i<mailRows.size();i++){
            user = userRepository.findById(user.getId()).get();
            if(emailValidation(user)) {
                if(isWithinWorkingHours()){
                    if(!checkIfResponse(user, mailRows.get(i))){
                        try{
                            sequenceList = sequenceListRepository.findById(sequenceList.getId()).get();
                        }catch (Exception e){
                            e.printStackTrace();
                            return;
                        }
                        try {
                            //Kollar om man stäng av den så stängs threaden
                            if(!sequenceList.isOngoing()){
                                System.out.println("thread cancelled. shutting down running thread.");
                                return;
                            }
                            sendSequenceEmail(mailRows.get(i), user, sequenceList, mailList);
                            mailRows.get(i).setSentDate(Date.valueOf(returnDateWithTime()));
                            mailRows.get(i).setSentSequence(true);
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
                        mailRows.get(i).setSentSequence(true);
                        mailRows.get(i).setSentDate(Date.valueOf(returnDateWithTime()));
                        mailRowRepository.save(mailRows.get(i));
                    }
                }
                else{
                    sequenceList.setOngoing(false);
                    sequenceListRepository.save(sequenceList);
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

        LocalTime startOfWorkDay = LocalTime.of(6, 0);
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
        System.out.println("Sent message successfully for User=" + user.getEmail() + ", Interval=" + mailList.getIntervalPeriod() + "s, To=" + mailRow.getEmail());
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
        System.out.println("Sent sequence successfully for user " + user.getEmail() + ". Interval=" + mailList.getIntervalPeriod() + "s");
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
    public static boolean checkIfResponse(User user, MailRow mailRow) {
        try {
            Properties properties = new Properties();

            properties.put("mail.pop3.host", user.getMailHost());
            properties.put("mail.pop3.port", "995");

            Session emailSession = Session.getInstance(properties,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(user.getMailEmail(), user.getMailPassword());
                        }
                    });

            Store store = emailSession.getStore("pop3s");
            store.connect(user.getMailHost(), user.getMailEmail(), user.getMailPassword());

            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            int totalMessages = emailFolder.getMessageCount();
            int startMessage = Math.max(1, totalMessages - 200);  // adjust this value based on how many recent emails you want to fetch

            for (int i = totalMessages; i >= startMessage; i--) {
                Message message = emailFolder.getMessage(i);
                if (extractEmail(message.getFrom()).contains(mailRow.getEmail())) {
                    java.util.Date receivedDate = message.getSentDate();
                    java.util.Date sentDate = new java.util.Date(mailRow.getSentDate().getTime());

                    // Normalize dates to ignore time components
                    LocalDate receivedLocalDate = receivedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate sentLocalDate = sentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    if(receivedLocalDate.isAfter(sentLocalDate) || receivedLocalDate.isEqual(sentLocalDate)) {
                        return true;
                    }
                    return false;
                }
            }

            emailFolder.close(false);
            store.close();
        } catch (NoSuchProviderException nspe) {
            nspe.printStackTrace();
        } catch (MessagingException me) {
            me.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    //

    //Handles all the leads generation functions
    @Transactional
    public void constructCompaniesFromJson() throws IOException, LoginException, InterruptedException {
        final String baseUrl = "https://www.allabolag.se/lista/aktiebolag/24";
        List<String> countyPaths = returnCountyPath();
        List<String> branchPaths = returnBranchPath();
        ArrayList<Company> companies = new ArrayList<>();
        ArrayList<String> orgNoList = new ArrayList<>();
        int totalResults = getTotalHits(baseUrl);

        for (int i = 0; i < countyPaths.size(); i++) {
            for (int j = 0; j < branchPaths.size(); j++) {
                String countyPath = countyPaths.get(i).replaceAll("Ä", "%C3%84")
                        .replaceAll("Å", "%C3%85")
                        .replaceAll("Ö", "%C3%96");
                String branchPath = branchPaths.get(j).replaceAll("Ä", "%C3%84")
                        .replaceAll("Å", "%C3%85")
                        .replaceAll("Ö", "%C3%96");

                String url = baseUrl + branchPath + countyPath;
                int hits = getTotalHits(url);
                int pages = (int) Math.ceil(hits / 20.0);

                System.out.printf("[%d/%d] | [%d/%d] | [%d/%d]\n", i, countyPaths.size(), j, branchPaths.size(), companies.size(), totalResults);

                if (pages <= 400) {
                    companies.addAll(getCompaniesFromUrl(url, pages));
                } else {
                    companies.addAll(getCompaniesFromUrlDetailedByRevenue(url));
                }
                orgNoList.addAll(companies.stream()
                        .map(Company::getOrgNo)
                        .collect(Collectors.toList()));

                System.gc();
            }
        }
        saveCompanies(companies);
        // Check if there are any REMOVED old companies from last update batch
        List<Company> removedCompanies = companyRepository.findAllByOrgNoNotIn(orgNoList);

        // This might cause an error, as it uses delete
        companyRepository.deleteAll(removedCompanies);

        BatchUpdater batchUpdater = batchUpdaterRepository.findAll().get(0);
        batchUpdater.setDateUpdated(Date.valueOf(returnDateWithTime()));
        batchUpdater.setUpdating(false);
        batchUpdaterRepository.save(batchUpdater);
    }
    public void saveCompanies(List<Company> companies) {
        // Fetch all existing companies in a single operation
        List<String> orgNos = companies.stream()
                .map(Company::getOrgNo)
                .collect(Collectors.toList());
        Map<String, Company> existingCompaniesMap = companyRepository.findByOrgNoIn(orgNos)
                .stream()
                .collect(Collectors.toMap(Company::getOrgNo, Function.identity()));

        // Process companies in-memory
        for (int i = 0; i < companies.size(); i++) {
            System.out.println("iterating new/removed companies... " + (i + 1) + "/" + companies.size());

            Company existing = existingCompaniesMap.get(companies.get(i).getOrgNo());

            if (existing == null) {
                companyRepository.save(companies.get(i));
            } else {
                existing.setAssets(companies.get(i).getAssets());
                existing.setCounty(companies.get(i).getCounty());
                existing.setExecutive(companies.get(i).getExecutive());
                existing.setVisitAdress(companies.get(i).getVisitAdress());
                existing.setRevenue(companies.get(i).getRevenue());
                existing.setCity(companies.get(i).getCity());
                existing.setCmpName(companies.get(i).getCmpName());
                existing.setHasRemarks(companies.get(i).isHasRemarks());
                existing.setTelephone(companies.get(i).getTelephone());
                existing.setHBranch(companies.get(i).getHBranch());
                existing.setLinkTo(companies.get(i).getLinkTo());
                existing.setPostalAdress(companies.get(i).getPostalAdress());
                existing.setRegDate(companies.get(i).getRegDate());
                existing.setUBranch(companies.get(i).getUBranch());
                companyRepository.save(existing);
            }
        }
    }
    public ArrayList<String> returnCountyPath(){
        ArrayList<String> countyPath = new ArrayList<>();
        countyPath.add("/xl/9");
        countyPath.add("/xl/10");
        countyPath.add("/xl/23");
        countyPath.add("/xl/19");
        countyPath.add("/xl/7");
        countyPath.add("/xl/8");
        countyPath.add("/xl/4");
        countyPath.add("/xl/18");
        countyPath.add("/xl/22");
        countyPath.add("/xl/21");
        countyPath.add("/xl/25");
        countyPath.add("/xl/17");
        countyPath.add("/xl/24");
        countyPath.add("/xl/20");
        countyPath.add("/xl/6");
        countyPath.add("/xl/3");
        countyPath.add("/xl/13");
        countyPath.add("/xl/5");
        countyPath.add("/xl/12");
        countyPath.add("/xl/14");
        countyPath.add("/xl/1");
        return countyPath;
    }
    public ArrayList<String> returnBranchPath(){
        ArrayList<String> branchPath = new ArrayList<>();
        branchPath.add("/xv/BYGG-,%20DESIGN-%20&%20INREDNINGSVERKSAMHET");
        branchPath.add("/xv/JURIDIK,%20EKONOMI%20&%20KONSULTTJÄNSTER");
        branchPath.add("/xv/FASTIGHETSVERKSAMHET");
        branchPath.add("/xv/DATA,%20IT%20&%20TELEKOMMUNIKATION");
        branchPath.add("/xv/DETALJHANDEL");
        branchPath.add("/xv/PARTIHANDEL");
        branchPath.add("/xv/BANK,%20FINANS%20&%20FÖRSÄKRING");
        branchPath.add("/xv/HÄLSA%20&%20SJUKVÅRD");
        branchPath.add("/xv/HOTELL%20&%20RESTAURANG");
        branchPath.add("/xv/TILLVERKNING%20&%20INDUSTRI");
        branchPath.add("/xv/TEKNISK%20KONSULTVERKSAMHET");
        branchPath.add("/xv/TRANSPORT%20&%20MAGASINERING");
        branchPath.add("/xv/UTBILDNING,%20FORSKNING%20&%20UTVECKLING");
        branchPath.add("/xv/KULTUR,%20NÖJE%20&%20FRITID");
        branchPath.add("/24/xv/FÖRETAGSTJÄNSTER");
        branchPath.add("/xv/REPARATION%20&%20INSTALLATION");
        branchPath.add("/xv/JORDBRUK,%20SKOGSBRUK,%20JAKT%20&%20FISKE");
        branchPath.add("/xv/REKLAM,%20PR%20&%20MARKNADSUNDERSÖKNING");
        branchPath.add("/xv/MEDIA");
        branchPath.add("/xv/HÅR%20&%20SKÖNHETSVÅRD");
        branchPath.add("/xv/BEMANNING%20&%20ARBETSFÖRMEDLING");
        branchPath.add("/xv/ÖVRIGA%20KONSUMENTTJÄNSTER");
        branchPath.add("/xv/MOTORFORDONSHANDEL");
        branchPath.add("/xv/UTHYRNING%20&%20LEASING");
        branchPath.add("/xv/LIVSMEDELSFRAMSTÄLLNING");
        branchPath.add("/xv/AVLOPP,%20AVFALL,%20EL%20&%20VATTEN");
        branchPath.add("/xv/RESEBYRÅ%20&%20TURISM");
        branchPath.add("/xv/BRANSCH-,%20ARBETSGIVAR-%20&%20YRKESORG.");
        branchPath.add("/xv/OFFENTLIG%20FÖRVALTNING%20&%20SAMHÄLLE");
        branchPath.add("/xv/AMBASSADER%20&%20INTERNATIONELLA%20ORG.");
        return branchPath;
    }
    public int getTotalHits(String path) throws IOException {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("accept", "application/json");
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String output;

        StringBuffer response = new StringBuffer();
        while ((output = in.readLine()) != null) {

            response.append(output);
        }
        in.close();
        JsonElement json = JsonParser.parseString(response.toString());
        JsonObject jsonObject = json.getAsJsonObject();
        return Integer.parseInt(jsonObject.get("totalHits").toString());
    }
    public ArrayList getCompaniesFromUrl(String path, int pages) {
        ArrayList<Company> companies = new ArrayList<>();
        for(int i = 1; i<pages+1;i++){
            try {
                URL url = new URL(path + "?page=" + i);
                System.out.println("PAGE:[" + i + "/" + pages + "]"  + " | " + "[" +  path + "]");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestProperty("accept", "application/json");
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String output;

                StringBuffer response = new StringBuffer();
                while ((output = in.readLine()) != null) {

                    response.append(output);
                }
                in.close();
                JsonElement json = JsonParser.parseString(response.toString());
                JsonObject jsonObject = json.getAsJsonObject();
                JsonArray companiesJson = jsonObject.get("hitlistVue").getAsJsonArray();
                Gson g = new Gson();
                for (JsonElement companyItem : companiesJson) {
                    Company company = g.fromJson(companyItem, Company.class);
                    companies.add(company);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return companies;
    }
    public ArrayList getCompaniesFromUrlDetailedByRevenue(String path) throws IOException, LoginException, InterruptedException {
        ArrayList<Company> companies = new ArrayList<>();
        ArrayList<String> revenue = returnRevenuePath();
        for(int i = 0; i< revenue.size();i++){
            int totalResult = getTotalHits(path + revenue.get(i));
            int pages = (int) Math.ceil(totalResult / 20.0);
            for(int c = 1; c<pages+1;c++){
                try {
                    URL url = new URL(path + revenue.get(i) + "?page=" + c);
                    System.out.println("PAGE:[" + c + "/" + pages + "]"  + " | " + "[" +  path + revenue.get(i) + "]");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestProperty("accept", "application/json");
                    conn.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String output;

                    StringBuffer response = new StringBuffer();
                    while ((output = in.readLine()) != null) {

                        response.append(output);
                    }
                    in.close();
                    JsonElement json = JsonParser.parseString(response.toString());
                    JsonObject jsonObject = json.getAsJsonObject();
                    JsonArray companiesJson = jsonObject.get("hitlistVue").getAsJsonArray();
                    Gson g = new Gson();
                    for (JsonElement companyItem : companiesJson) {
                        Company company = g.fromJson(companyItem, Company.class);
                        companies.add(company);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return companies;
    }
    public ArrayList<String> returnRevenuePath(){
        ArrayList<String> revenuePath = new ArrayList<>();
        revenuePath.add("/xr/-1");
        revenuePath.add("/xr/1-100");
        revenuePath.add("/xr/101-200");
        revenuePath.add("/xr/201-300");
        revenuePath.add("/xr/301-400");
        revenuePath.add("/xr/401-500");
        revenuePath.add("/xr/501-600");
        revenuePath.add("/xr/601-700");
        revenuePath.add("/xr/701-800");
        revenuePath.add("/xr/801-900");
        revenuePath.add("/xr/901-1000");
        revenuePath.add("/xr/1001-2000");
        revenuePath.add("/xr/2001-3000");
        revenuePath.add("/xr/3001-4000");
        revenuePath.add("/xr/4001-5000");
        revenuePath.add("/xr/5001-6000");
        revenuePath.add("/xr/6001-7000");
        revenuePath.add("/xr/7001-8000");
        revenuePath.add("/xr/8001-9000");
        revenuePath.add("/xr/9001-10000");
        revenuePath.add("/xr/10001-");
        return revenuePath;
    }
    public void getCmpInfo(Company company) {
        Document doc = getDocumentFromUrl("https://www.allabolag.se/" + company.getLinkTo());

        company.setExecutive(extractSingleField(doc, "#company-card_overview > div.cc-flex-grid > div:nth-child(1) > dl > dd:nth-child(2) > a"));
        company.setRevenue(extractLongField(doc, "#company-card_overview > div.accountfigures_container > div.company-account-figures > div.table__container > table > tbody > tr:nth-child(1) > td:nth-child(2)"));
        company.setAssets(extractLongField(doc, "#company-card_overview > div.accountfigures_container > div.company-account-figures > div.table__container > table > tbody > tr:nth-child(4) > td:nth-child(2)"));
        company.setTelephone(extractTelephone(doc));
        company.setVisitAdress(extractAddressField(doc));
        company.setPostalAdress(extractPostAdress(doc));
        company.setCounty(extractCounty(doc));
        company.setRegDate(extractRegDate(doc));
        company.setUpdatedInfo(Date.valueOf(returnDateWithTime()));
        companyRepository.save(company);
    }
    private Document getDocumentFromUrl(String url) {
        try {
            return Jsoup.connect(url)
                    .maxBodySize(0)
                    .userAgent(randomUserAgent())
                    .referrer("https://duckduckgo.com")
                    .get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private String extractSingleField(Document doc, String cssQuery) {
        try {
            return doc.selectFirst(cssQuery).text();
        } catch (Exception e) {
            return null;
        }
    }
    private Long extractLongField(Document doc, String cssQuery) {
        try {
            return Long.parseLong(doc.selectFirst(cssQuery).text().replaceAll(" ",""))*1000;
        } catch (Exception e) {
            return 0L;
        }
    }
    private String extractTelephone(Document doc) {
        try {
            String field = doc.selectFirst("#company-card_overview > div.cc-flex-grid > div:nth-child(2) > dl").text();
            return field.contains("Telefon ") ? field.split("Telefon ")[1].split(" ")[0] : null;
        } catch (Exception e) {
            return null;
        }
    }
    private String extractCounty(Document doc) {
        try {
            String field = doc.selectFirst("#company-card_overview > div.cc-flex-grid > div:nth-child(2) > dl").text();
            return field.contains("Län ") ? field.split("Län ")[1] : null;
        } catch (Exception e) {
            return null;
        }
    }
    private String extractPostAdress(Document doc) {
        try {
            String field = doc.selectFirst("#company-card_overview > div.cc-flex-grid > div:nth-child(2) > dl").text();
            return field.contains("Ort ") ? field.split("Ort ")[1].split(" Län")[0] : null;
        } catch (Exception e) {
            return null;
        }
    }
    private String extractRegDate(Document doc) {
        try {
            String field = doc.selectFirst("#company-card_overview > div.cc-flex-grid > div:nth-child(1) > dl").text();
            return field.contains("Registreringsår ") ? field.split("Registreringsår ")[1].split(" ")[0] : null;
        } catch (Exception e) {
            return null;
        }
    }
    private String extractAddressField(Document doc) {
        try {
            String visitAdress = doc.selectFirst("#company-card_overview > div.cc-flex-grid > div:nth-child(2) > dl").text();
            if(visitAdress.contains("Besöksadress ")){
                int startIdx = visitAdress.indexOf("Besöksadress ") + "Besöksadress ".length();
                String address = visitAdress.substring(startIdx);
                if(address.contains(" Ort")) {
                    int endIdx = address.indexOf(" Ort");
                    address = address.substring(0, endIdx);

                    // Check if address is duplicated
                    int midIndex = address.length() / 2;
                    String firstHalf = address.substring(0, midIndex).trim();
                    String secondHalf = address.substring(midIndex).trim();

                    if (firstHalf.equals(secondHalf)) {
                        address = firstHalf;
                    }
                    return address;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public String randomUserAgent(){
        ArrayList<String> userAgent = new ArrayList<>();
        userAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Mobile/15E148 Safari/604.1");
        userAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
        userAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:102.0) Gecko/20100101 Firefox/102.0");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.114 Safari/537.36 Edg/103.0.1264.49");
        userAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15");
        userAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/103.0.5060.63 Mobile/15E148 Safari/604.1");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36");
        userAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.54 Safari/537.36");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; Trident/7.0; rv:11.0) like Gecko");
        userAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
        userAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36 Keeper/1616028983");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.53 Safari/537.36 Edg/103.0.1264.37");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.66 Safari/537.36 Edg/103.0.1264.44");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/537.36 Edg/103.0.1264.77");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/537.36");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/E7FBAF");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.114 Safari/537.36");
        userAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 15_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.6 Mobile/15E148 Safari/604.1");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/E7FBAF");
        userAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 15_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1");
        userAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:98.0) Gecko/20100101 Firefox/98.0");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; WebView/3.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.19044");
        userAgent.add("GSA/13.27.8.26.arm64");
        userAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.38 Safari/537.36");
        userAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:102.0) Gecko/20100101 Firefox/102.0");
        userAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Safari/605.1.15");
        userAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 14_8_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1");
        for (int i = 0; i < userAgent.size(); i++)
        {
            // generating the index using Math.random()
            int index = (int)(Math.random() * userAgent.size());

            return userAgent.get(index);
        }
        return null;
    }
    public void getCmpWebsiteEmail(Company company) throws InterruptedException, IOException, LoginException {
        //I denna method måste vi vara försiktiga att inte bli avstängda från sökresultaten. Det är fortfarande lite oklart varför man blir det. För snabba sökningar? Att det är en bot? Etc...
        String website = returnLink(company.getCmpName());
        if (website != null) {
            company.setWebsite(website);
        }
        if(company.getWebsite() != null){
            String mostCommonEmail = fetchMostCommonEmail(returnUrlsToScrape(website));
            if (mostCommonEmail != null) {
                System.out.println("found " + mostCommonEmail + " at " + website + "...");
                company.setEmail(mostCommonEmail);
            }
        }

        company.setUpdatedWebsite(Date.valueOf(returnDateWithTime()));
        companyRepository.save(company);
    }
    public ArrayList<String> returnUrlsToScrape(String url) throws IOException, LoginException, InterruptedException {
        ArrayList<String> urlsToScrape = initializeUrlsList(url);
        if (urlsToScrape.isEmpty()) {
            return urlsToScrape;
        }

        Document document = getDocumentFromUrlWebsite(url);

        if (document != null) {
            Elements allLinks = document.getElementsByTag("a");
            urlsToScrape = extractUrls(urlsToScrape, allLinks, url);
        }
        return removeDuplicates(urlsToScrape);
    }
    private ArrayList<String> initializeUrlsList(String url) throws IOException, InterruptedException, LoginException {
        ArrayList<String> urlsToScrape = new ArrayList<>();
        urlsToScrape.add(url);
        try {
            if(!pingHost(url)) {
                if(pingHost(url.replaceAll("http://", "https://"))){
                    url = url.replaceAll("http://", "https://");
                }
                else{
                    return urlsToScrape;
                }
            }
            Document document = Jsoup.connect(url)
                    .timeout(10000)
                    .maxBodySize(0)
                    .userAgent(randomUserAgent())
                    .referrer("https://duckduckgo.com")
                    .get();

            Elements allLinks = document.getElementsByTag("a");
            int i = 0;
            for (Element link : allLinks) {
                if (i == 10) {
                    break;
                }
                String relativeUrl = link.attr("href");
                if (relativeUrl.contains(url)) {
                    urlsToScrape.add(relativeUrl);
                } else {
                    try {
                        String firstChar = String.valueOf(relativeUrl.charAt(0));
                        if (!relativeUrl.contains("http") && firstChar.contentEquals("/") && !relativeUrl.contains("www.")) {
                            String hybridLink = url + relativeUrl;
                            if (hybridLink.contains(url)) {
                                urlsToScrape.add(hybridLink);
                            }
                        }
                    } catch (Exception e) {

                    }
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return removeDuplicates(urlsToScrape);
    }
    private Document getDocumentFromUrlWebsite(String url) {
        try {
            return Jsoup.connect(url)
                    .timeout(10000)
                    .maxBodySize(0)
                    .userAgent(randomUserAgent())
                    .referrer("https://duckduckgo.com")
                    .get();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private ArrayList<String> extractUrls(ArrayList<String> urlsToScrape, Elements allLinks, String url) {
        int linkCount = 0;

        for (Element link : allLinks) {
            if (linkCount == 10) {
                break;
            }
            String relativeUrl = link.attr("href");

            if (relativeUrl.contains(url)) {
                urlsToScrape.add(relativeUrl);
            } else if (isRelativeUrl(relativeUrl)) {
                String hybridLink = url + relativeUrl;
                if (hybridLink.contains(url)) {
                    urlsToScrape.add(hybridLink);
                }
            }
            linkCount++;
        }

        return urlsToScrape;
    }
    private boolean isRelativeUrl(String url) {
        if (url.contains("http") || url.contains("www.")) {
            return false;
        }
        try {
            return String.valueOf(url.charAt(0)).equals("/");
        } catch (Exception e) {
            return false;
        }
    }
    public static <T> ArrayList<T> removeDuplicates(ArrayList<T> list) throws LoginException, InterruptedException {
        ArrayList<T> newList = new ArrayList<T>();
        for (T element : list) {
            if (!newList.contains(element)) {
                newList.add(element);
            }
        }
        return newList;
    }
    public String fetchMostCommonEmail(ArrayList<String> urls) throws IOException {
        ArrayList<String> commonEmails = new ArrayList<>();
        String regex = "[a-zA-Z0-9\\.\\-\\_]+@[a-zA-Z]+[\\.]{1}[a-zA-Z]{2,4}";
        Pattern pattern = Pattern.compile(regex);
        for(int i = 0; i<urls.size();i++){
            try{
                Document doc = Jsoup.connect(urls.get(i))
                        .timeout(10000)
                        .maxBodySize(0)
                        .userAgent(randomUserAgent())
                        .referrer("https://duckduckgo.com")
                        .get();

                String body[] = doc.text().replaceAll("\n", " ").split(" ");
                for(int c = 0; c<body.length;c++){
                    if(patternMatches(body[c])){
                        Matcher matcher = pattern.matcher(body[c]);
                        while(matcher.find()){
                            commonEmails.add(matcher.group());
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(commonEmails.size()!=0) {
            return mostCommon(commonEmails);
        }
        else return null;
    }
    public static boolean patternMatches(String emailAddress) {
        return Pattern.compile("^(.+)@(\\S+)$")
                .matcher(emailAddress)
                .matches();
    }
    public static <T> T mostCommon(List<T> list) {
        Map<T, Integer> map = new HashMap<>();

        for (T t : list) {
            Integer val = map.get(t);
            map.put(t, val == null ? 1 : val + 1);
        }

        Map.Entry<T, Integer> max = null;

        for (Map.Entry<T, Integer> e : map.entrySet()) {
            if (max == null || e.getValue() > max.getValue())
                max = e;
        }
        return max.getKey();
    }
    public String returnLink(String cmpName) throws IOException, InterruptedException {
        Document doc = Jsoup.connect("https://duckduckgo.com/html/?q=" + cmpName.replaceAll(" ", "+").replaceAll("&", "%26").replaceAll("ö", "%C3%B6").replaceAll("ä", "%C3%A4").replaceAll("å","%C3%A5") + "&kl=se-sv")
                .timeout(10000)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Charset","ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Cache-Control", "max-age=0")
                .header("sec-ch-ua-platform", "macOS")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"99\", \"Google Chrome\";v=\"99\"")
                .header("Upgrade-Insecure-Requests", "1")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:98.0) Gecko/20100101 Firefox/98.0")
                .referrer("https://www.duckduckgo.com/")
                .get();
        Elements elements = doc.select("#links > div.results_links");
        int i = 0;
        for(Element row : elements){
            if(i==3){
                break;
            }
            if(row.text().contentEquals("No results.")){
                System.out.println("[ERROR : NO RESULTS]");
            }
            String link = getAuthorityUrl(row.selectFirst("div > div > a").text());
            if(link != null && !stringContains(link, bannedUrls())){
                return returnAbsoluteUrl(getAuthorityUrl(row.selectFirst("div > div > a").text()));
            }
            i++;
        }
        return null;
    }
    public static String returnAbsoluteUrl(String url) throws IOException, InterruptedException {
        if(url.length()>2){
            if(url.contains("more info")){
                return null;
            }
            if (!url.startsWith("http://")) {
                String defUrlHttp = "http://" + url;
                if(pingHost(defUrlHttp)){
                    return defUrlHttp;
                }
                else{
                    if(defUrlHttp.contains("www.")){
                        if(pingHost(defUrlHttp.replaceAll("www\\.", ""))){
                            return defUrlHttp.replaceAll("www\\.", "");
                        }
                    }
                    else{
                        if(pingHost(defUrlHttp.replaceAll("http://", "http://www."))){
                            return defUrlHttp.replaceAll("http://", "http://www.");
                        }
                    }
                }
            }
            if(!url.startsWith("https://")){
                String defUrlHttps = "https://" + url;
                if(pingHost(defUrlHttps)){
                    return defUrlHttps;
                }
                else{
                    if(defUrlHttps.contains("www.")){
                        if(pingHost(defUrlHttps.replaceAll("www\\.", ""))){
                            return defUrlHttps.replaceAll("www\\.", "");
                        }
                    }
                    else{
                        if(pingHost(defUrlHttps.replaceAll("https://", "https://www."))){
                            return defUrlHttps.replaceAll("https://", "https://www.");
                        }
                    }
                }
            }
            return null;
        }
        return null;
    }
    public static String getAuthorityUrl(String urlString) throws MalformedURLException {
        if (!urlString.startsWith("http://")) {
            urlString = "http://" + urlString;
        }
        URL url= new URL(urlString);
        return url.getAuthority();
    }
    public static boolean stringContains(String s, ArrayList<String> bannedUrls){
        for(int i = 0; i<bannedUrls.size();i++){
            if(s.contains(bannedUrls.get(i))){
                return true;
            }
        }
        return false;
    }
    public static ArrayList<String> bannedUrls(){
        ArrayList<String> bannedUrls = new ArrayList<>();
        bannedUrls.add("allbiz");
        bannedUrls.add("chargebax");
        bannedUrls.add("opencorporates");
        bannedUrls.add("yelp");
        bannedUrls.add("tripadvisor");
        bannedUrls.add("largestcompanies");
        bannedUrls.add("sparbankerna");
        bannedUrls.add("sakochliv");
        bannedUrls.add("thebanks");
        bannedUrls.add("infoisinfo");
        bannedUrls.add("öppettider");
        bannedUrls.add("news.");
        bannedUrls.add("dnb");
        bannedUrls.add("brabyggare");
        bannedUrls.add("blocket");
        bannedUrls.add("upplysningar");
        bannedUrls.add("sites.google");
        bannedUrls.add("kompass");
        bannedUrls.add("bygg.se");
        bannedUrls.add("solidinfo");
        bannedUrls.add("offerta");
        bannedUrls.add("byggtjanst");
        bannedUrls.add("foretagsfakta");
        bannedUrls.add("facebook");
        bannedUrls.add("allabolag");
        bannedUrls.add("kreditrapporten");
        bannedUrls.add("purehelp");
        bannedUrls.add("bolagsfakta");
        bannedUrls.add("linkedin");
        bannedUrls.add("wikipedia");
        bannedUrls.add("ledigajobb-göteborg");
        bannedUrls.add("redovisningskonsulten");
        bannedUrls.add("hitta");
        bannedUrls.add("svd");
        bannedUrls.add("bizzdo");
        bannedUrls.add("ratsit");
        bannedUrls.add("guldbolag");
        bannedUrls.add("proff");
        bannedUrls.add("eniro");
        bannedUrls.add("merinfo");
        bannedUrls.add("cylex");
        bannedUrls.add("helagotland");
        bannedUrls.add("infobelpro");
        return bannedUrls;
    }
    public static boolean pingHost(String url) throws IOException, InterruptedException {
        try {
            Document document = Jsoup.connect(url).get();
            return true;
        } catch (Exception e) {
            if(e.toString().contains("403")){
                return true;
            }
            return false;
        }
    }
    //
}
