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
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.OutputStream;
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
    private static UserRepository staticUserRepository;

    @Autowired
    MailListRepository mailListRepository;
    private static MailListRepository staticMailListRepository;

    @Autowired
    MailRowRepository mailRowRepository;
    private static MailRowRepository staticMailRowRepository;

    @Autowired
    SequenceListRepository sequenceListRepository;
    private static SequenceListRepository staticSequenceListRepository;


    @PostConstruct
    public void init() throws LoginException {
        staticUserRepository = userRepository;
        staticMailListRepository = mailListRepository;
        staticMailRowRepository = mailRowRepository;
        staticSequenceListRepository = sequenceListRepository;
    }
    @GetMapping("/")
    public String home(){
        return "redirect:/user/dashboard";
    }

    @GetMapping("/user/redigera-utskick/{id}")
    public String editList(@PathVariable long id, Model model){
        Pageable pageable = PageRequest.of(0, 1);
        User user = returnCurrentUser();
        model.addAttribute("user", user);
        MailList mailList = mailListRepository.findById(id);
        Page<MailRow> mailRow = mailRowRepository.findByMailListIdAndIsHeader(mailList.getId(), true, pageable);
        Page<MailRow> firstRow = mailRowRepository.findByMailListIdAndIsHeader(mailList.getId(), false, pageable);
        ArrayList<SequenceList> sequenceLists = sequenceListRepository.findByMailListId(mailList.getId());
        ArrayList<HeaderValues> headerValues = new ArrayList<>();
        ArrayList<DataRow> dataRows = new ArrayList<>();
        String[] mailRows = mailRow.getContent().get(0).getDataRow().split(",");
        String[] firstRowString = firstRow.getContent().get(0).getDataRow().split(",");
        for(int i = 0; i<mailRows.length;i++){
            HeaderValues headerValue = new HeaderValues();
            headerValue.setIndex(i);
            headerValue.setName(mailRows[i].replaceAll(" ", "").toLowerCase());
            headerValues.add(headerValue);
        }
        for(int i = 0; i<firstRowString.length;i++){
            DataRow dataRow = new DataRow();
            dataRow.setIndex(i);
            dataRow.setName(firstRowString[i]);
            dataRows.add(dataRow);
        }
        model.addAttribute("headersJson", parseHeadersToJson(headerValues));
        model.addAttribute("firstRowJson", parseDataRowToJson(dataRows));
        model.addAttribute("headers", headerValues);
        model.addAttribute("mailList", mailList);
        model.addAttribute("sequenceList", sequenceLists);
        return "edit-list";
    }
    @GetMapping("/user/dashboard")
    public String dashboard(Model model){
        User user = returnCurrentUser();
        addDashboardAttributes(model, user);
        model.addAttribute("user", user);
        return "dashboard";
    }
    public void addDashboardAttributes(Model model, User user){
        Pageable pageable = PageRequest.of(0, 5);
        Page<MailList> mailListsNotFinished = mailListRepository.findAllByFinishedAndUserIdAndFinishedUploadingIsTrueOrderByDispatchDate(false, user.getId(), pageable);
        Page<MailList> mailListsFinished = mailListRepository.findAllByFinishedAndUserIdAndFinishedUploadingIsTrueOrderByDispatchDate(true, user.getId(), pageable);
        model.addAttribute("mailListsFinished", mailListsFinished);
        model.addAttribute("mailListsFinishedTotal", mailListsFinished.getTotalElements());

        model.addAttribute("mailListsNotFinished", mailListsNotFinished);
        model.addAttribute("mailListsNotFinishedTotal", mailListsNotFinished.getTotalElements());
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/user/koade-utskick")
    public String queuedList(@RequestParam("page") int page, Model model){
        User user = returnCurrentUser();
        model.addAttribute("user", user);
        Pageable pageable = PageRequest.of(page, 15);
        Page<MailList> queuedLists = mailListRepository.findAllByFinishedAndUserIdAndFinishedUploadingIsTrueOrderByDispatchDate(false, user.getId(), pageable);
        model.addAttribute("queuedLists", queuedLists);
        return "queued-list";
    }
    @GetMapping("/user/fardiga-utskick")
    public String completedList(@RequestParam("page") int page, Model model){
        User user = returnCurrentUser();
        model.addAttribute("user", user);
        Pageable pageable = PageRequest.of(page, 15);
        Page<MailList> completedLists = mailListRepository.findAllByFinishedAndUserIdAndFinishedUploadingIsTrueOrderByDispatchDate(true, user.getId(), pageable);
        model.addAttribute("completedLists", completedLists);
        return "finished-list";
    }
    @GetMapping("/skapa-konto")
    public String createAccount(Model model){
        model.addAttribute("userObject", new User());
        return "create-account";
    }
    @GetMapping("/user/ladda-ner-utskick/{id}")
    public void downloadFile(Model model, @PathVariable long id, HttpServletResponse response) throws IOException {
        MailList mailList = mailListRepository.findById(id);
        ArrayList<MailRow> mailRows = mailRowRepository.findByMailListId(mailList.getId());

        String filename = "file.csv";
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        OutputStream outputStream = response.getOutputStream();

        for (int i = 0; i < mailRows.size(); i++) {
            String row = mailRows.get(i).getDataRow();
            outputStream.write(row.getBytes());
            outputStream.write("\n".getBytes());
        }

        outputStream.flush();
        outputStream.close();
    }
    @GetMapping("/user/ladda-ner-utskick-fardig/{id}")
    public void downloadFileFinished(Model model, @PathVariable long id, HttpServletResponse response) throws IOException {
        MailList mailList = mailListRepository.findById(id);
        ArrayList<MailRow> mailRows = mailRowRepository.findByMailListId(mailList.getId());

        String filename = "file.csv";
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        OutputStream outputStream = response.getOutputStream();

        for (int i = 0; i < mailRows.size(); i++) {
            String row = mailRows.get(i).getDataRow();
            outputStream.write(row.getBytes());
            outputStream.write("\n".getBytes());
        }

        outputStream.flush();
        outputStream.close();
    }
    @PostMapping("/skapa-konto")
    public String createAccount(Model model, @Valid @ModelAttribute("userObject") User user, HttpServletRequest request){
        user.setPassword(servDao.enCryptedPassword(user));
        userRepository.save(user);
        return "redirect:/user/dashboard";
    }
    @PostMapping("/user/mark-complete")
    public String markComplete(Model model, HttpServletRequest request){
        long id = Long.parseLong(request.getParameter("id"));
        MailList mailList = mailListRepository.findById(id);
        mailList.setFinished(true);
        mailListRepository.save(mailList);
        return "redirect:/user/koade-utskick?page=0";
    }
    @PostMapping("/user/removelist")
    public String removeList(Model model, HttpServletRequest request){
        long id = Long.parseLong(request.getParameter("id"));
        MailList mailList = mailListRepository.findById(id);
        ArrayList<MailRow> mailRows = mailRowRepository.findByMailListId(mailList.getId());
        ArrayList<SequenceList> sequenceLists = sequenceListRepository.findByMailListId(mailList.getId());
        sequenceListRepository.deleteAll(sequenceLists);
        mailRowRepository.deleteAll(mailRows);
        mailListRepository.delete(mailList);
        return "redirect:/user/koade-utskick?page=0";
    }
    @PostMapping("/user/removelist-finished")
    public String removeListFinished(Model model, HttpServletRequest request){
        long id = Long.parseLong(request.getParameter("id"));
        MailList mailList = mailListRepository.findById(id);
        ArrayList<MailRow> mailRows = mailRowRepository.findByMailListId(mailList.getId());
        ArrayList<SequenceList> sequenceLists = sequenceListRepository.findByMailListId(mailList.getId());
        sequenceListRepository.deleteAll(sequenceLists);
        mailRowRepository.deleteAll(mailRows);
        mailListRepository.delete(mailList);
        return "redirect:/user/fardiga-utskick?page=0";
    }

    @PostMapping("/user/reinstate-list")
    public String reinstateList(Model model, HttpServletRequest request){
        long id = Long.parseLong(request.getParameter("id"));
        String date = request.getParameter("date");
        MailList mailList = mailListRepository.findById(id);
        mailList.setFinished(false);
        mailList.setDispatchDate(Date.valueOf(date));
        mailListRepository.save(mailList);
        return "redirect:/user/fardiga-utskick?page=0";
    }
    @PostMapping("/user/settings/update")
    public String updateSettings(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes){
        User user = returnCurrentUser();
        String email = request.getParameter("email");
        String alias = request.getParameter("alias");
        String password = request.getParameter("password");
        String host = request.getParameter("host");
        String port = request.getParameter("port");
        int interval = Integer.parseInt("interval");
        user.setMailEmail(email);
        user.setMailAlias(alias);
        user.setIntervalPeriod(interval);
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
        ArrayList<HeaderValues> headerValues = parseDataRowToHeader(parseCSVRows(completeData, 0, request.getParameter("separator"), true));
        ArrayList<DataRow> firstRow = parseCSVRows(completeData, 1, request.getParameter("separator"), false);

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
        String footerContent = request.getParameter("footerContent");
        String title = request.getParameter("title");
        String completeData = request.getParameter("completeData");

        //Lists to split the data
        ArrayList<String> sequenceContentList = new ArrayList<>();
        ArrayList<String> sequenceAfterList = new ArrayList<>();
        ArrayList<String> titleSequenceList = new ArrayList<>();

        //List of actual objects
        ArrayList<SequenceList> sequenceLists = new ArrayList<>();
        MailList mailList = new MailList();

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
        mailList.setFileName(fileName);
        mailList.setFooterContent(footerContent);
        mailList.setDispatchDate(Date.valueOf(dispatchDate));
        mailList.setMainContent(mainContent);
        mailList.setTitle(title);
        mailList.setUserId(user.getId());
        MailList savedMailList = mailListRepository.save(mailList);

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

        //Save the rows in a new thread so the user does not have to wait
        new Thread(asyncSaveRows(completeData, user, savedMailList)).start();

        redirectAttributes.addFlashAttribute("uploaded", true);
        return "redirect:/user/dashboard";
    }
    public Runnable asyncSaveRows(String completeData, User user, MailList mailList){
        return new Runnable() {
            @SneakyThrows
            public void run() {
                ArrayList<MailRow> rows = new ArrayList<>();
                //MailRow
                CSVReader reader = new CSVReaderBuilder(
                        new StringReader(completeData))
                        .withSkipLines(0)
                        .build();

                List<String[]> r = reader.readAll();
                for(int i = 0; i<r.size();i++){
                    String csvLine = String.join(",", r.get(i));
                    MailRow mailRow = new MailRow();
                    mailRow.setDataRow(csvLine);
                    mailRow.setUserId(user.getId());
                    mailRow.setMailListId(mailList.getId());
                    if(i==0){
                        mailRow.setHeader(true);
                    }
                    rows.add(mailRow);
                }
                staticMailRowRepository.saveAll(rows);
                mailList.setFinishedUploading(true);
                staticMailListRepository.save(mailList);
            }
        };
    }
    @PostMapping("/user/edit-list/{id}")
    public String editListComplete(Model model, HttpServletRequest request, @PathVariable long id) throws CsvException, IOException {
        String fileName = request.getParameter("name");
        String dispatchDate = request.getParameter("date");
        String mainContent = request.getParameter("mainContent");
        String footerContent = request.getParameter("footerContent");
        String title = request.getParameter("title");

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
        MailList mailList = mailListRepository.findById(id);
        mailList.setFileName(fileName);
        mailList.setFooterContent(footerContent);
        mailList.setDispatchDate(Date.valueOf(dispatchDate));
        mailList.setMainContent(mainContent);
        mailList.setTitle(title);
        mailList.setUserId(user.getId());
        mailListRepository.save(mailList);

        ArrayList<SequenceList> oldSequenceLists = sequenceListRepository.findByMailListId(mailList.getId());
        System.out.println("deleting: " + oldSequenceLists.size() + " items");
        sequenceListRepository.deleteAll(oldSequenceLists);

        ArrayList<SequenceList> newSequenceLists = new ArrayList<>();
        //SequenceList
        for(int i = 0; i<sequenceContentList.size();i++){
            SequenceList sequenceList = new SequenceList();
            sequenceList.setMailListId(mailList.getId());
            sequenceList.setTitle(titleSequenceList.get(i));
            sequenceList.setMainContent(sequenceContentList.get(i));
            sequenceList.setSequenceStartDate(Date.valueOf(sequenceAfterList.get(i)));
            sequenceList.setUserId(user.getId());
            newSequenceLists.add(sequenceList);
        }
        sequenceListRepository.saveAll(newSequenceLists);
        return "redirect:/user/koade-utskick?page=0";
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
                dataRow.setName(firstLine[i].replaceAll(" ", ""));
            }
            else{
                dataRow.setName(firstLine[i]);
            }
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