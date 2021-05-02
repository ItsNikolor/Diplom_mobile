package com.example.service.resources;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.example.service.ChangeVar;
import com.example.service.GameStart;
import com.example.service.MainActivity;
import com.example.service.OutHandlerThread;
import com.example.service.R;
import com.example.service.WaitingMenuClient;
import com.example.service.WaitingMenuHost;
import com.example.service.pageView.FragmentVar;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import static java.lang.Math.min;
import static java.util.Collections.shuffle;

public class GameInfo{
    public static final String START_NAME = "!";
    private static final String TAG = "MyDebug";
    static final String ENTER = "{";
    public static final String SEP = "#";
    public static GameInfo game=null;
    private static String[] loose_cond,win_cond;

    public long start_time, current_time, round_length;
    public String cur_action = "",cur_ans = "";

    public boolean isHost;
    public boolean isConnecting;
    public Context main_context;
    public Handler mainHandler;
    public List<OutHandlerThread> outHandlers;

    public String scenarioPath, roomName,scenarioName;
    public List<Client> clients;

    public HashMap<String,Var> vars;
    public HashMap<String,Tab> tabs;
    public HashMap<String,Role> roles;
    public HashMap<String,Action> additional_actions;

    public String log_journal = "";
    public String log_journal_sep = "";

    public ViewPager main_pager;
    public String hostAdr;
    public int leader_id;
    public int width;
    public int height;
    private int max_round;
    public int cur_round=0;
    public boolean isLeader = false;

    public static GameInfo getInstance(){
        if (game==null){
            game = new GameInfo();
        }
        return game;
    }

    Runnable timer_runnable = new Runnable() {
        @Override
        public void run() {
            current_time = System.currentTimeMillis();
            FragmentVar.update_timer();

            if(current_time-start_time>=round_length){
                if(isHost)
                    next_round(false);
                return;
                //mainHandler.removeCallbacks(this);
            }
            mainHandler.postDelayed(this,250);
        }
    };

    public static void send_action(String action_id, String cur_ans,int before_round, boolean now) {
        if(game.isHost){
            if(now){
                Action action = game.roles.get(action_id.split("_")[0]).actions.get(action_id);
                game.perform_action(action,cur_ans,true);
                game.check_end();
                for (Var v:game.vars.values()) if(v.visibility) game.print_all(v.full_str());

                for (int i=1;i<game.clients.size();i++){
                    if(game.clients.get(i).alive){
                        for (Action a: game.roles.get(game.clients.get(i).role_id).actions.values()) {
                            game.outHandlers.get(i).print(a.available_string());
                        }
                    }
                }

                for (String key:game.additional_actions.keySet()){
                    if(!game.additional_actions.get(key).available())
                        game.outHandlers.get(game.leader_id).print("-ka"+SEP+key);
                }

                game.print_all("-d"+SEP+game.cur_round);
                GameStart.fragments.get(1).onResume();
            }
            else{
                game.cur_action = action_id;
                game.cur_ans = cur_ans;
                game.next_round(true);
            }
        }
        else{
            if(before_round!=game.cur_round)
                return;
            game.outHandlers.get(0).print("send"+SEP+action_id+SEP+cur_ans+SEP+before_round);
        }
    }

    void start_timer(){
        if(round_length==0) return;
        if(isHost)
            start_time = System.currentTimeMillis();

        mainHandler.post(timer_runnable);
    }

    void perform_action(Action a,String ans,boolean host){
        Pair<Double, HashMap<String, Double>> t = compute_action(a.req);
        if(host||((a.use_n==0 || a.count_use<a.use_n)&&t.first!=0)){
            if(!host)
                assign(t.second);
            a.count_use += 1;
            a.current_ans = ans;

            if(log_journal_sep.equals(""))
                log_journal_sep = ((FragmentVar)GameStart.fragments.get(1)).sep();
            log_journal += a.descr+'\n'+log_journal_sep+'\n';
            print_all("log_journal"+SEP+a.descr);


            for(int i=0;i<a.cond.size();i++){
                t = compute_action(a.cond.get(i),a.id);
                if (t.first!=0){
                    t = compute_action(a.action.get(i),a.id);
                    assign(t.second);
                }
            }

        }
    }

    private void next_round(boolean host) {
        cur_round += 1;
        additional_actions.clear();
        vars.get("ptime").value = cur_round;
        mainHandler.removeCallbacks(timer_runnable);
        //Выполнить действие
        if(!cur_action.equals("")){
            Action a = roles.get(cur_action.split("_")[0]).actions.get(cur_action);
            perform_action(a,cur_ans,host);
            cur_action = "";
            cur_ans = "";
        }

        //Проверить win и lose
        check_end();
        //Выполнить функции
        for(Var v:vars.values()){
            for(Func f:v.funcs){
                Pair<Double, HashMap<String, Double>> t = compute_action(f.cond);
                if(t.first!=0){
                    t = compute_action(f.func);
                    assign(t.second);
                    break;
                }
            }
        }
        //Проверить win и lose
        check_end();
        if(cur_round>=max_round){
            loose();
            return;
        }
        //Отправить всем новые значения переменных
        for (Var v:vars.values()) if(v.visibility) print_all(v.toString());

        for (int i=1;i<clients.size();i++){
            if(clients.get(i).alive){
                for (Action a: roles.get(clients.get(i).role_id).actions.values()) {
                    outHandlers.get(i).print(a.available_string());
                }
            }
        }


        start_timer();
        print_all("start_time"+SEP+start_time+SEP+round_length);

        GameStart.fragments.get(1).onResume();
        print_all("-d"+SEP+cur_round);

        if(!ChangeVar.var_id.equals("")) {
            Var var = GameInfo.game.vars.get(ChangeVar.var_id);
            ChangeVar.var_value.setText(var.name + "  " + var.value);
        }

    }

    private void check_end() {
        Pair<Double, HashMap<String, Double>> t = compute_action(win_cond);
        if(t.first!=0){
            win();
        }

        t = compute_action(loose_cond);
        if(t.first!=0){
            loose();
        }

    }

    private void loose() {
        log_journal += "Вы проиграли"+'\n'+log_journal_sep+'\n';
        print_all("log_journal"+SEP+"Вы проиграли");

        System.out.println("Вы проиграли");
    }

    private void win() {
        log_journal += "Вы победили"+'\n'+log_journal_sep+'\n';
        print_all("log_journal"+SEP+"Вы победили");
        System.out.println("Вы победили");

    }

    public void clear(){
        vars = new HashMap<>();
        tabs = new HashMap<>();
        roles = new HashMap<>();
        additional_actions = new HashMap<>();

        clients = new ArrayList<>(256);
        outHandlers = new ArrayList<>(256);

        log_journal = "";
    }

    public void init(String scenarioPath) throws IOException {
        clear();

        File check_file = new File(Environment.getExternalStorageDirectory(), scenarioPath);
        if (check_file.exists()) this.scenarioPath = check_file.getAbsolutePath();
        else this.scenarioPath = scenarioPath;

        File scenarioFile = new File(this.scenarioPath);

        BufferedReader br = new BufferedReader(new FileReader(
                scenarioFile));

        try {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("Начало "+line+"Конец");
                GameInfo.parse_line(line,new MyInputStream(br),-1);
            }
        } finally {
            br.close();
        }

        String imagePath = this.scenarioPath.substring(0, this.scenarioPath.lastIndexOf('/'))+
                "/Images";
        System.out.println(TAG +":  "+ imagePath);
        File[] images = (new File(imagePath)).listFiles();
        for (File f:images){
            System.out.println(TAG +":  "+ f.getName());

            String[] names = f.getName().split(" ");

            boolean visibility = names[1].charAt(0)=='1';
            String image_id = names[0];
            String tab_id = image_id.substring(0, image_id.indexOf('_'));

            System.out.println(TAG +":  "+ image_id);
            System.out.println(TAG +":  "+ tab_id);


            Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(f));

            int width = bmp.getWidth();
            int height = bmp.getHeight();

            double k_w = ((double)GameInfo.game.width) / width;
            double k_h = ((double)GameInfo.game.height) / height;
            double k = Math.min(k_w,k_h);

            if (k<1)
                bmp = Bitmap.createScaledBitmap(bmp, (int)(width*k), (int)(height*k), true);

            visibility = tabs.get(tab_id).visibility || visibility;
            Image image = new Image(tab_id,image_id,visibility,bmp);

            tabs.get(tab_id).images.put(image_id,image);

            if(visibility) tabs.get(tab_id).count_visible++;
        }
        for(Tab tab:tabs.values()){
            if (tab.count_visible == tab.images.size())
                tab.visibility = true;
        }
    }

    public static class MyInputStream{
        DataInputStream dis;
        BufferedReader br;
        boolean is_br;

        String readUTF() throws IOException {
            if(is_br)
                return br.readLine();
            else
                return dis.readUTF();
        }

        public MyInputStream(BufferedReader br) {
            this.br = br;
            is_br = true;
        }

        public MyInputStream(DataInputStream dis) {
            this.dis = dis;
            is_br = false;
        }
    }

    public static void parse_line(String line,
                                  MyInputStream in,
                                  int id) throws IOException {
        if(line.equals("")) return;

        line = line + "m";
        String[] l = line.replace(ENTER,"\n").split(SEP);
        l[l.length-1] = l[l.length-1].substring(0,l[l.length-1].length()-1);

        switch (l[0]){
            case "-n":
                game.scenarioName = l[1];
                if (!game.isHost)
                    game.mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            WaitingMenuClient.scenario_name.setText(game.scenarioName);
                        }
                    });
                return;
            case "-v":
                if (l.length==3){
                    game.vars.get(l[1]).value = Float.parseFloat(l[2]);
                }
                else {
                    Var v = new Var(l[1],l[6], Float.parseFloat(l[2]),
                            Float.parseFloat(l[3]),
                            Float.parseFloat(l[4]),
                            l[5].equals("1"));
                    if(v.id.equals("ptime")){
                        game.round_length = ((long) v.value)*1000;
                        game.round_length = 120*1000;//Заменить
                        v.value = 0;
                        game.max_round = (int)v.maxValue;
                        if(game.max_round==0)
                            game.max_round = 999999;
                        v.visibility = false;
                    }
                    game.vars.put(l[1], v);
                }
                return;
            case "-t":
                if(game.tabs.containsKey(l[1]))
                    return;

                Tab t = new Tab(l[1],l[3],
                        l[2].equals("1"));
                game.tabs.put(l[1], t);

                return;
            case "-r":
                Role r = new Role(l[1],l[3],l[2]);
                game.roles.put(l[1], r);

                if (!game.isHost)
                    WaitingMenuClient.add_role(r);

                return;
            case "-f":
                Func func = new Func(empty_split(l[2]," "),empty_split(l[3]," "));
                game.vars.get(l[1].split("_")[0]).funcs.add(func);
                return;
            case "-a":
                Action action = new Action(l[1],Integer.parseInt(l[2]),l[3],
                        empty_split(l[4]," "),empty_split(l[5],","),
                        empty_split(l[6],","));
                int num_a = Integer.parseInt(l[7]);

                for (int i=0;i<num_a;i++){
                    line = in.readUTF();
                    System.out.println(line);
                    l = (line+'m').split(SEP);
                    l[1] = l[1].substring(0,l[1].length()-1);
                    action.action.add(empty_split(l[0]," "));
                    action.cond.add(empty_split(l[1]," "));
                }
                game.roles.get(action.id.split("_")[0]).actions.put(action.id,action);

                return;
            case "-p":
                int p_id = (game.isHost?id:Integer.parseInt(l[1]));
                while (game.clients.size()<=p_id)
                    game.clients.add(new Client(game.clients.size()));

                if (l.length == 2)
                    game.clients.set(p_id,new Client(p_id));
                else
                    game.clients.set(p_id,new Client(p_id,l[3],l[2]));

                if (!game.isHost)
                    WaitingMenuClient.add_client();
                else{
                    WaitingMenuHost.add_client();
                    game.print_all(game.clients.get(p_id).toString());
                }
                return;
            case "-b":
                game.mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(game.main_context, GameStart.class);
                        game.main_context.startActivity(intent);
                    }
                });
                return;
            case "-d":
                int cur_round = Integer.parseInt(l[1]);
                if(cur_round==game.cur_round){
                    if(!game.cur_action.equals("") &&
                    !game.roles.get(game.cur_action.split("_")[0]).actions.get(game.cur_action).can_use)
                        game.cur_action = "";
                }
                else{
                    game.cur_action = "";
                    game.additional_actions.clear();
                }
                game.cur_round = cur_round;
                game.mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Fragment f:GameStart.fragments) f.onResume();
                    }
                });
                return;
            case "-i":
                int length = Integer.parseInt(l[4]);

                DataInputStream dis = in.dis;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                int readed = 0;
                byte[] buffer = new byte[2048];
                while(readed<length){
                    int read = dis.read(buffer, 0, (int) min(buffer.length, length - readed));
                    readed += read;
                    stream.write(buffer,0,read);
                }
                System.out.println("Client readed " + readed);
                byte[] bytes = stream.toByteArray();
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                int width = bmp.getWidth();
                int height = bmp.getHeight();

                double k_w = ((double)GameInfo.game.width) / width;
                double k_h = ((double)GameInfo.game.height) / height;
                double k = Math.min(k_w,k_h);

                if (k<1)
                    bmp = Bitmap.createScaledBitmap(bmp, (int)(width*k), (int)(height*k), true);

                game.tabs.get(l[1]).images.put(l[2],new Image(l[1],
                        l[2],
                        l[3].equals("1"),
                        bmp));
                return;
            case "start_time":
                game.start_time = Long.parseLong(l[1]);
                game.round_length = Long.parseLong(l[2]);
                game.start_timer();
                return;
            case "-l":
                loose_cond = empty_split(l[1]," ");
                return;
            case "-w":
                win_cond = empty_split(l[1]," ");
                return;
            case "log_journal":
                if(game.log_journal_sep.equals(""))
                    game.log_journal_sep = ((FragmentVar)GameStart.fragments.get(1)).sep();
                GameInfo.game.log_journal += l[1]+'\n'+game.log_journal_sep+'\n';
                return;
            case "-available":
                game.roles.get(l[1].split("_")[0]).actions.get(l[1]).can_use = Boolean.parseBoolean(l[2]);
                return;
            case "send":
                String[] finalL = l;
                game.mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(Integer.parseInt(finalL[3])!=game.cur_round)
                            return;

                        if(game.isHost) {
                            Action tmp_a = game.roles.get(finalL[1].split("_")[0]).actions.get(finalL[1]);
                            if (!tmp_a.available())
                                return;
                            game.additional_actions.put(Integer.toString(id), tmp_a);

                            if(id==game.leader_id){
                                game.cur_action = finalL[1];
                                game.cur_ans = finalL[2];

                                game.next_round(false);
                            }
                            else{
                                game.outHandlers.get(id).print("confirm"+SEP+finalL[1]);
                                game.outHandlers.get(game.leader_id).print("send"+SEP+ finalL[1]+SEP+ finalL[2]+SEP+ finalL[3]+
                                        SEP+id+SEP+game.clients.get(id).name+SEP+ tmp_a.descr+SEP+
                                                TextUtils.join(",",tmp_a.ans)+SEP+
                                        TextUtils.join(",",tmp_a.ans_id));
//                                        String.join(",",tmp_a.ans)+SEP+
//                                        String.join(",",tmp_a.ans_id));
                            }
                        }
                        else{//leader
                            Action tmp_a = new Action(finalL[1],0, finalL[6],new String[0],
                                    empty_split(finalL[7],","),empty_split(finalL[8],","));
                            tmp_a.player_name = finalL[5];
                            tmp_a.current_ans = finalL[2];
                            game.additional_actions.put(finalL[4],tmp_a);
                            game.mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    GameStart.fragments.get(0).onResume();
                                }
                            });
                        }
                    }
                });
                return;
            case "-ka":
                game.additional_actions.remove(l[1]);
                return;
            case "confirm":
                game.cur_action = l[1];
                game.mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        GameStart.fragments.get(1).onResume();
                    }
                });
                return;
            case "leader":
                game.isLeader = true;
                return;
        }
    }

    private static String[] empty_split(String target, String split) {
        if(target.equals("")) return new String[0];
        else return target.split(split);
    }

    public void print_all(String s){
        for (int i=1;i<outHandlers.size();i++){
            outHandlers.get(i).print(s);
        }
    }

    public void print_all_image(Image image){
        for (int i=1;i<outHandlers.size();i++){
            outHandlers.get(i).print_image(image);
        }
    }

    public void add_client(Socket client) throws IOException {
        final int id = clients.size();
        clients.add(new Client(id,"Без имени",Role.no_role.id,client));
        WaitingMenuHost.add_client();

        outHandlers.add(new OutHandlerThread(client));

        outHandlers.get(id).print("-n"+SEP+game.scenarioName);
        for (Role r:roles.values()) if(!r.id.equals(Role.host_role.id)) outHandlers.get(id).print(r.toString());
        for (Client c: clients) outHandlers.get(id).print(c.toString());
        print_all(clients.get(id).toString());

        Thread inThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DataInputStream in = new DataInputStream(client.getInputStream());

                    while (true){
                        String line = in.readUTF();
                        if (line==null) break;

                        System.out.println("Host in " + line);

                        GameInfo.parse_line(line,new MyInputStream(in),id);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    System.out.printf("Host died!!!!!!!!!!!!!!!!!!!!!!!!!");
                    GameInfo.game.outHandlers.get(id).handlerThread.quit();
                    clients.get(id).alive = false;
                    print_all(clients.get(id).ded_str());

                    WaitingMenuHost.add_client();
                }
            }
        });
        inThread.start();
    }

    public void start_game(Context context) throws IOException {
        leader_id = -1;
        for (int i=1;i<clients.size();i++) {
            Client c = clients.get(i);
            if(c.alive && c.role_id.equals(Role.leader_role.id))
                leader_id = i;
            if (c.alive && c.role_id.equals(Role.no_role.id)) {
                Toast.makeText(context, "Не все игроки выбрали роль", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if(leader_id==-1){
            Toast.makeText(context, "Кто-то обязательно должен быть лидером", Toast.LENGTH_SHORT).show();
            return;
        }

        WaitingMenuHost.listener.close();

        outHandlers.get(leader_id).print("leader");
        print_all("-b");
        for (Var v:vars.values()) if(v.visibility) print_all(v.full_str());
        for (Tab t:tabs.values()) {
            if (t.visibility) {
                print_all(t.toString());
                for (Image i : t.images.values()) {
                    print_all(i.toString());
                    print_all_image(i);
                }
            } else {
                boolean tab_sent = false;
                for (Image i : t.images.values()) {
                    if(i.visibility) {
                        if (!tab_sent) {
                            print_all(t.toString());
                            tab_sent = true;
                        }
                        print_all(i.toString());
                        print_all_image(i);
                    }
                }
            }
        }
        for (int i=1;i<clients.size();i++){
            if(clients.get(i).alive){
                for (Action a: roles.get(clients.get(i).role_id).actions.values()) {
                    outHandlers.get(i).print(a.toString());
                    outHandlers.get(i).print(a.available_string());
                }
            }
        }
        start_timer();
        print_all("start_time"+SEP+start_time+SEP+round_length);
        print_all("-d"+SEP+cur_round);

        System.out.println(TAG +":  "+ "end of start");

        Intent intent = new Intent(context, GameStart.class);
        context.startActivity(intent);
    }

    public Pair<Double,HashMap<String,Double>> compute_action(String []s){
        return compute_action(s,"");
    }
    public Pair<Double,HashMap<String,Double>> compute_action(String [] s, String action_id){
        HashMap<String,Double> ass = new HashMap<>();
        Stack<String> stack = new Stack<>();

        if(s.length==0) return new Pair<>(1.,ass);
        for (String c:s) {
            if (!ops.contains(c)) {
                stack.push(c);
                continue;
            }

            if (c.equals("+")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = a + b;

                stack.push(Double.toString(ans));
            } else if (c.equals("-")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = a - b;

                stack.push(Double.toString(ans));
            } else if (c.equals("*")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = a * b;

                stack.push(Double.toString(ans));
            } else if (c.equals("/")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = a / b;

                stack.push(Double.toString(ans));
            } else if (c.equals("//")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = (int)(a / b);

                stack.push(Double.toString(ans));
            } else if (c.equals("%")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = a % b;

                stack.push(Double.toString(ans));
            } else if (c.equals("<")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = (a < b)?1:0;

                stack.push(Double.toString(ans));
            } else if (c.equals(">")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = (a > b)?1:0;

                stack.push(Double.toString(ans));
            } else if (c.equals(">=")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = (a >= b)?1:0;

                stack.push(Double.toString(ans));
            } else if (c.equals("<=")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = (a <= b)?1:0;

                stack.push(Double.toString(ans));
            } else if (c.equals("=")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = (a == b)?1:0;

                stack.push(Double.toString(ans));
            } else if (c.equals("max")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = Math.max(a,b);

                stack.push(Double.toString(ans));
            } else if (c.equals("min")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = min(a,b);

                stack.push(Double.toString(ans));
            } else if (c.equals("&")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = ((a==1)&(b==1))?1:0;

                stack.push(Double.toString(ans));
            } else if (c.equals("|")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = ((a==1)|(b==1))?1:0;

                stack.push(Double.toString(ans));
            } else if (c.equals("minus")) {
                double a = take_value(stack.pop());
                double ans = -a;

                stack.push(Double.toString(ans));
            } else if (c.equals("not")) {
                double a = take_value(stack.pop());
                double ans = (a==0)?1:0;

                stack.push(Double.toString(ans));
            } else if (c.equals("exp")) {
                double b = take_value(stack.pop());
                double a = take_value(stack.pop());
                double ans = Math.pow(a,b);

                stack.push(Double.toString(ans));
            } else if (c.equals("ans")) {
                String a = stack.pop();
                String ans = roles.get(a.split("_")[0]).actions.get(a).current_ans;
                if (ans.equals(""))
                    ans = "-1";
                else
                    ans = ans.substring(2);

                stack.push(ans);
            } else if (c.equals("show")) {
                //String tabs_id[]  = empty_split(stack.pop(),",");
                String tabs_id[] = stack.pop().split(",");
                int a = (int) take_value(stack.pop());

                List<String> activate_id = new ArrayList<>();

                for (String t:tabs_id){
                    if(tabs.containsKey(t)){
                        if(!tabs.get(t).visibility) {
                            for (Image i : tabs.get(t).images.values()) {
                                if (!i.visibility) {
                                    activate_id.add(i.image_id);
                                }
                            }
                        }
                    }
                    else if(!tabs.get(t.split("_")[0]).images.get(t).visibility){
                        activate_id.add(t);
                    }
                }
                shuffle(activate_id);
                if(a==0)
                    a = activate_id.size();
                for(int i=0;i<min(a,activate_id.size());i++){
                    String t = activate_id.get(i);

                    Tab tab = tabs.get(t.split("_")[0]);

                    tab.images.get(t).visibility = true;
                    tab.add_visible();

                    print_all(tab.toString());
                    print_all(tab.images.get(t).toString());
                    print_all_image(tab.images.get(t));
                }
                stack.push("1");
            } else if (c.equals("an")) {
                String ans;
                if (action_id.equals("")){
                    ans = "-1";
                }
                else{
                    ans = roles.get(action_id.split("_")[0]).actions.get(action_id).current_ans;
                    if (ans.equals(""))
                        ans = "-1";
                    else
                        ans = ans.substring(2);
                }

                stack.push(ans);
            } else if (c.equals("eq")) {
                double b = take_value(stack.pop());
                String a = stack.pop();
                Var v = vars.get(a);

                boolean ans = (b>=v.minValue) && (b<=v.maxValue);

                stack.push(ans?"1":"0");
                ass.put(a,b);
            }
        }
        assert (stack.size()==1);
        return new Pair<>(take_value(stack.pop()),ass);
    }

    void assign(HashMap<String,Double> ass){
        for (String ass_key:ass.keySet()){
            Double value = ass.get(ass_key);
            vars.get(ass_key).set_value(value);
        }
    }

    private double take_value(String s) {
        switch (s.charAt(0)){
            case 'p':
                return vars.get(s).value;
            case 't':
                if (s.contains("_"))
                    return tabs.get(s.split("_")[0]).images.get(s).visibility?1:0;

                return tabs.get(s).visibility?1:0;
            case 'r':
                if (s.contains("_"))
                    return roles.get(s.split("_")[0]).actions.get(s).count_use;
                else {
                    double role_counter = 0;
                    for (Client c : clients) {
                        if (c.alive & (s.equals(c.role_id) | s.equals(Role.host_role.id)))
                            role_counter++;
                    }
                    return role_counter;
                }

            default:
                return Double.parseDouble(s);
        }
    }

    static Set<String> ops = new HashSet<String>(Arrays.asList(
            new String[]{"+", "-","*","/","//","%","&","|",
            "<","<=",">",">=","=","eq","max","min","show","ans","an",
            "not","exp"}));
}
