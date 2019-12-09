package edu.nyu.cs6083.nextdoor.controller;


import edu.nyu.cs6083.nextdoor.bean.Message;
import edu.nyu.cs6083.nextdoor.bean.User;
import edu.nyu.cs6083.nextdoor.dao.MessageDao;
import edu.nyu.cs6083.nextdoor.dao.ThreadDao;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {


    @Autowired
    ThreadDao threadDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MessageDao messageDao;

    @GetMapping("")
    public String ping(HttpServletRequest request, Model m) {
        if (request.getSession().getAttribute("useradmin") == null) {
            m.addAttribute("user", new User());
            return "index";
        }

        return "redirect:/main";

    }


    @GetMapping("main")
    public String mainPage(HttpServletRequest request, Model m) {
        User user = (User) request.getSession().getAttribute("useradmin");

        // initial message mid
        String sql = "Select min(mid) as mid from message Where tid in (Select tid From message Where message.timestamp > (Select lastlogouttime From user Where uid = ?1) and tid in (Select tid From threadparticipant natural join thread Where recid = ?2 and type = ?3))\n";

        //type 3 block
        List<Integer> type3 = new ArrayList<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, user.getUid());
            ps.setInt(2, user.getUid());
            ps.setInt(3, 3);
            return ps;
        }, rs -> {
            type3.add(rs.getInt("mid"));
        });
        List<Message> type3s = messageDao.findAllById(type3);

        // type 2 hood
        List<Integer> type2 = new ArrayList<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, user.getUid());
            ps.setInt(2, user.getUid());
            ps.setInt(3, 2);
            return ps;
        }, rs -> {
            type2.add(rs.getInt("mid"));
        });
        List<Message> type2s = messageDao.findAllById(type2);
//
//
        //type 1 friends
        List<Integer> type1 = new ArrayList<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, user.getUid());
            ps.setInt(2, user.getUid());
            ps.setInt(3, 1);
            return ps;
        }, rs -> {
            type1.add(rs.getInt("mid"));
        });
        List<Message> type1s = messageDao.findAllById(type1);

        //type 0 neighbors
        List<Integer> type0 = new ArrayList<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, user.getUid());
            ps.setInt(2, user.getUid());
            ps.setInt(3, 0);
            return ps;
        }, rs -> {
            type0.add(rs.getInt("mid"));
        });
        List<Message> type0s = messageDao.findAllById(type0);

        m.addAttribute("t3",type3s);
        m.addAttribute("t2",type2s);
        m.addAttribute("t1",type1s);
        m.addAttribute("t0",type0s);
        return "main/main";
    }

//    @Autowired
//    MessageDao dao;
//
//    @GetMapping("/ping")
//    public void pong() {
//
//        List<Message> message = dao.findAll();
//
//        for (Message m : message) {
//            System.out.println(m);
//        }
//    }
}
