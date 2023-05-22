package com.balumbo.blb.controller;


import com.balumbo.blb.Service.UserService;
import com.balumbo.blb.model.MailList;
import com.balumbo.blb.model.MailRow;
import com.balumbo.blb.model.SequenceList;
import com.balumbo.blb.model.User;
import com.balumbo.blb.objects.DataRow;
import com.balumbo.blb.objects.HeaderValues;
import com.balumbo.blb.repository.MailListRepository;
import com.balumbo.blb.repository.MailRowRepository;
import com.balumbo.blb.repository.SequenceListRepository;
import com.balumbo.blb.repository.UserRepository;
import com.balumbo.blb.security.CustomUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;


@Controller
public class UserController {

    @Autowired
    private UserService servDao;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MailListRepository mailListRepository;

    @Autowired
    MailRowRepository mailRowRepository;

    @Autowired
    SequenceListRepository sequenceListRepository;


    @GetMapping("/")
    public String home(){
        return "redirect:/user/dashboard";
    }
    @GetMapping("/user/dashboard")
    public String dashboard(Model model){
        User user = returnCurrentUser();
        addDashboardAttributes(model, user);
        model.addAttribute("user", user);
        return "dashboard";
    }
    public void addDashboardAttributes(Model model, User user){
        ArrayList<MailList> mailListsFinished = mailListRepository.findAllByFinishedAndUserId(true, user.getId());
        ArrayList<MailList> mailListsNotFinished = mailListRepository.findAllByFinishedAndUserId(false, user.getId());
        model.addAttribute("mailListsFinished", mailListsFinished);
        model.addAttribute("mailListsNotFinished", mailListsNotFinished);
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }
    @GetMapping("/skapa-konto")
    public String createAccount(Model model){
        model.addAttribute("userObject", new User());
        return "create-account";
    }
    @PostMapping("/skapa-konto")
    public String createAccount(Model model, @Valid @ModelAttribute("userObject") User user, HttpServletRequest request){
        user.setPassword(servDao.enCryptedPassword(user));
        userRepository.save(user);
        return "redirect:/user/dashboard";
    }
    @PostMapping("/user/settings/update")
    public String updateSettings(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes){
        User user = returnCurrentUser();
        String email = request.getParameter("email");
        String alias = request.getParameter("alias");
        String password = request.getParameter("password");
        String host = request.getParameter("host");
        String port = request.getParameter("port");
        user.setMailEmail(email);
        user.setMailAlias(alias);
        user.setMailPassword(password);
        user.setMailHost(host);
        user.setMailPort(port);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("hasError", false);
        return "redirect:/user/dashboard";
    }
    @RequestMapping(value=("/user/upload-list"),headers=("content-type=multipart/*"),method= RequestMethod.POST)
    public String uploadList(HttpServletRequest request, Model model, @RequestParam("file") MultipartFile file) throws IOException, CsvException {
        byte[] bytes = file.getBytes();
        String completeData = new String(bytes);
        //Parse the csv
        ArrayList<HeaderValues> headerValues = parseDataRowToHeader(parseCSVRows(completeData, 0, request.getParameter("separator")));
        ArrayList<DataRow> firstRow = parseCSVRows(completeData, 1, request.getParameter("separator"));

        model.addAttribute("headers", headerValues);
        model.addAttribute("headersJson", parseHeadersToJson(headerValues));
        model.addAttribute("firstRowJson", parseDataRowToJson(firstRow));
        model.addAttribute("completeData", completeData);
        model.addAttribute("name", request.getParameter("name"));
        model.addAttribute("user", returnCurrentUser());
        return "list-content";
    }
    @PostMapping("/user/upload-list/complete")
    public String uploadListComplete(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) throws CsvException, IOException {
        String fileName = request.getParameter("name");
        String dispatchDate = request.getParameter("date");
        String mainContent = request.getParameter("mainContent");
        String title = request.getParameter("title");
        String completeData = request.getParameter("completeData");

        ArrayList<String> sequenceContentList = new ArrayList<>();
        ArrayList<String> sequenceAfterList = new ArrayList<>();
        ArrayList<String> titleSequenceList = new ArrayList<>();

        String[] sequenceContent = request.getParameterValues("sequenceContent[]");
        String[] sequenceAfter = request.getParameterValues("sequenceAfter[]");
        String[] titleSequence = request.getParameterValues("titleSequence[]");

        if(sequenceContent != null){
            for(int i = 0; i<sequenceContent.length;i++){
                sequenceContentList.add(sequenceContent[i]);
            }
        }
        if(sequenceAfter != null){
            for(int i = 0; i<sequenceAfter.length;i++){
                sequenceAfterList.add(sequenceAfter[i]);
            }
        }
        if(titleSequence != null){
            for(int i = 0; i<titleSequence.length;i++){
                titleSequenceList.add(titleSequence[i]);
            }
        }
        User user = returnCurrentUser();
        //MailList
        MailList mailList = new MailList();
        mailList.setFileName(fileName);
        mailList.setDispatchDate(Date.valueOf(dispatchDate));
        mailList.setMainContent(mainContent);
        mailList.setTitle(title);
        mailList.setUserId(user.getId());
        MailList savedMailList = mailListRepository.save(mailList);
        System.out.println(savedMailList);


        ArrayList<MailRow> rows = new ArrayList<>();
        //MailRow
        CSVReader reader = new CSVReaderBuilder(
                new StringReader(completeData))
                .withSkipLines(1)
                .build();

        List<String[]> r = reader.readAll();
        for(int i = 0; i<r.size();i++){
            String csvLine = String.join(",", r.get(i));
            MailRow mailRow = new MailRow();
            mailRow.setDataRow(csvLine);
            mailRow.setUserId(user.getId());
            mailRow.setMailListId(savedMailList.getId());
            if(i==0){
                mailRow.setHeader(true);
            }
            rows.add(mailRow);
        }
        mailRowRepository.saveAll(rows);


        ArrayList<SequenceList> sequenceLists = new ArrayList<>();
        //SequenceList
        for(int i = 0; i<sequenceContentList.size();i++){
            SequenceList sequenceList = new SequenceList();
            sequenceList.setMailListId(savedMailList.getId());
            sequenceList.setTitle(titleSequenceList.get(i));
            sequenceList.setMainContent(sequenceContentList.get(i));
            sequenceList.setSequenceStartDate(Date.valueOf(sequenceAfterList.get(i)));
            sequenceList.setUserId(user.getId());
            sequenceLists.add(sequenceList);
        }
        sequenceListRepository.saveAll(sequenceLists);
        redirectAttributes.addFlashAttribute("uploaded", true);
        return "redirect:/user/dashboard";
    }
    public List<String> parseHeadersToJson(ArrayList<HeaderValues> headerValues){
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> jsonHeaders = headerValues.stream()
                .map(header -> {
                    try {
                        return objectMapper.writeValueAsString(header);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        return jsonHeaders;
    }
    public List<String> parseDataRowToJson(ArrayList<DataRow> dataRows){
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> jsonHeaders = dataRows.stream()
                .map(header -> {
                    try {
                        return objectMapper.writeValueAsString(header);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        return jsonHeaders;
    }
    public ArrayList<DataRow> parseCSVRows(String completeData, int skipLines, String separator) throws IOException, CsvException {
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
            dataRow.setName(firstLine[i].replaceAll(" ", ""));
            dataRow.setIndex(i);
            dataRowsList.add(dataRow);
        }
        return dataRowsList;
    }
    public ArrayList<HeaderValues> parseDataRowToHeader(ArrayList<DataRow> dataRows){
        ArrayList<HeaderValues> headerValuesList = new ArrayList<>();
        for(int i = 0; i<dataRows.size();i++){
            HeaderValues headerValues = new HeaderValues();
            headerValues.setName(dataRows.get(i).getName().toLowerCase());
            headerValues.setIndex(dataRows.get(i).getIndex());
            headerValuesList.add(headerValues);
        }
        return headerValuesList;
    }
    public User returnCurrentUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails customUser = (CustomUserDetails) auth.getPrincipal();
        User currentUser = userRepository.findByEmail(customUser.getUsername());
        return currentUser;
    }

}