package com.balumbo.blb.security;

import com.balumbo.blb.model.Blacklist;
import com.balumbo.blb.model.MailList;
import com.balumbo.blb.model.MailRow;
import com.balumbo.blb.model.User;
import com.balumbo.blb.objects.DataRow;
import com.balumbo.blb.repository.BlacklistRepository;
import com.balumbo.blb.repository.MailListRepository;
import com.balumbo.blb.repository.MailRowRepository;
import com.balumbo.blb.repository.UserRepository;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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
    private UserRepository userRepository;

    @Autowired
    private MailRowRepository mailRowRepository;

    @Autowired
    private BlacklistRepository blacklistRepository;

    @PostConstruct
    public void resetOngoing(){
        System.out.println("starting up. Resetting ongoing on unfinished lists with date after current.");
        ArrayList<MailList> mailLists = mailListRepository.findAllByFinishedAndDispatchDateEqualOrAfter(false, java.sql.Date.valueOf(LocalDate.now()));
        for(int i = 0; i<mailLists.size();i++){
            mailLists.get(i).setOngoing(false);
        }
        mailListRepository.saveAll(mailLists);
    }

    @Scheduled(fixedRate = 5000) // Check for changes in the mailList every 5 seconds
    public void sendEmails() {
        ArrayList<MailList> mailLists = mailListRepository.findAllByFinishedAndDispatchDateEqualOrAfterAndOngoing(false, false, java.sql.Date.valueOf(LocalDate.now()));
        for(int i = 0; i<mailLists.size();i++){
            applicationEventPublisher.publishEvent(new HandleMailListEvent(mailLists.get(i)));
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
            mailRows = mailRowRepository.findByMailListIdAndSentAndError(mailList.getId(), false, false);
        } else {
            mailRows = mailRowRepository.findByMailListIdAndSentAndErrorAndEmailNotIn(mailList.getId(), false, false, blackListEmails);
        }
        System.out.println(mailRows);
        mailList.setOngoing(true);
        if(mailRows.size()==0){
            mailList.setOngoing(false);
            mailList.setFinished(true);
        }
        mailListRepository.save(mailList);
        for(int i = 0; i<mailRows.size();i++){
            try{
                mailList = mailListRepository.findById(mailList.getId()).get();
                sendEmail(mailRows.get(i), user, mailList);
                mailRows.get(i).setSent(true);
                mailRowRepository.save(mailRows.get(i));
                Thread.sleep(mailList.getIntervalPeriod());
            }catch (Exception e){
                e.printStackTrace();
                mailRows.get(i).setError(true);
                mailRowRepository.save(mailRows.get(i));
            }
        }
        System.out.println("Mail list complete. Setting finished.");
        mailList.setOngoing(false);
        mailList.setFinished(true);
        mailListRepository.save(mailList);
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
        //Transport.send(message);
        System.out.println("Sent message successfully for user " + user.getEmail() + ". Interval=" + mailList.getIntervalPeriod() + "ms");
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
