package edu.nyu.cs6083.nextdoor.controller;

import edu.nyu.cs6083.nextdoor.bean.Message;
import edu.nyu.cs6083.nextdoor.bean.User;
import edu.nyu.cs6083.nextdoor.dao.MessageDao;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MessageController {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MessageDao messageDao;

    @GetMapping("message")
    public String message(HttpServletRequest request, Model m) {
        User user = (User) request.getSession().getAttribute("useradmin");

        String sql = "Select distinct tid \n"
            + "From message Where message.timestamp > (Select lastlogouttime From user Where uid = ?) \n"
            + "and tid in (Select tid From threadparticipant natural join thread Where recid = ? and type = ?)\n";

        //type 3 block
        List<Integer> type3 = new ArrayList<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, user.getUid());
            ps.setInt(2, user.getUid());
            ps.setInt(3, 3);
            return ps;
        }, rs -> {
            type3.add(rs.getInt("tid"));
        });
        List<Message> type3s = messageDao.findMsg(type3);

        // type 2 hood
        List<Integer> type2 = new ArrayList<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, user.getUid());
            ps.setInt(2, user.getUid());
            ps.setInt(3, 2);
            return ps;
        }, rs -> {
            type3.add(rs.getInt("tid"));
        });
        List<Message> type2s = messageDao.findMsg(type2);

        //type 1 friends
        List<Integer> type1 = new ArrayList<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, user.getUid());
            ps.setInt(2, user.getUid());
            ps.setInt(3, 1);
            return ps;
        }, rs -> {
            type3.add(rs.getInt("tid"));
        });

        List<Message> type1s = messageDao.findMsg(type1);
        m.addAttribute("tids", type3);
        return "main/main";
    }

    @PostMapping("/sendmessage")
    public String send(@RequestParam Map<String, String> data, HttpServletRequest request) {
        String state = data.get("select");
        User user = (User) request.getSession().getAttribute("useradmin");

        switch (state) {
            case "toneighbor":
                insertMessage(0, data, user);
                break;
            case "tofriend":
                insertMessage(1, data, user);
                break;
            case "allblock":
                insertMessage(3, data, user);
                break;
            case "allhood":
                insertMessage(2, data, user);
                break;
        }

        return "login200";
    }

    private void insertMessage(int type, Map<String, String> data, User user) {
        String s1 = "INSERT INTO `nextdoor`.`thread`(`subject`, `type`) VALUES (?, ?);";
        String s2 =
            "Insert into message (`tid`, `author`, `title`, `timestamp`, `text`, `lat`,`lng`) "
                + "Values (?, ?, ?, now(), ?, ?, ?)";
        String s3 = "Insert into threadparticipant values(?, ?)";

        int recvid = Integer.MIN_VALUE;

        jdbcTemplate.update(s1, data.get("title"), type);

        int tid = jdbcTemplate
            .queryForObject("select tid from thread where tid = (select max(tid) from thread)",
                Integer.class);

        jdbcTemplate.update(s2, tid, user.getUid(), data.get("title"), data.get("content"), 0, 0);

        if (type == 1) {
            recvid = Integer.valueOf(data.get("tofriend"));
            jdbcTemplate.update(s3, tid, recvid);
        } else if (type == 0) {
            recvid = Integer.valueOf(data.get("toneighbor"));
            jdbcTemplate.update(s3, tid, recvid);
        } else if (type == 2) {
            String s5 = "select uid from joinblock natural join block "
                + "where nid = (select nid from joinblock natural join block where uid = ?)";
            List<Integer> ids = new ArrayList<>();
            jdbcTemplate.query(con -> {
                PreparedStatement ps = con.prepareStatement(s5);
                ps.setInt(1, user.getUid());
                return ps;
            }, rs -> {
                ids.add(rs.getInt("uid"));
            });

            for (int id : ids) {
                jdbcTemplate.update(s3, tid, id);
            }

        } else if (type == 3) {
            String s4 = "select uid from joinblock where bid = (select bid from joinblock where uid = ?)";
            List<Integer> ids = new ArrayList<>();

            jdbcTemplate.query(con -> {
                PreparedStatement ps = con.prepareStatement(s4);
                ps.setInt(1, user.getUid());
                return ps;
            }, rs -> {
                ids.add(rs.getInt("uid"));
            });

            for (int id : ids) {
                jdbcTemplate.update(s3, tid, id);
            }
        }


    }
}
