package com.unipi.sam.getnotes.table;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class User implements Serializable {
    public static final float serialVersionUID = 1;

    private Info info = new Info();
    private HashMap<String, Group.Info> myGroups = new HashMap<>();

    public User() {
    }

    public User(String id, String name) {
        this.info.id = id;
        this.info.name = name;
    }

    @Exclude
    public String getName() {
        return this.info.name;
    }

    @Exclude
    public String getId() {
        return this.info.id;
    }

    public HashMap<String, Group.Info> getMyGroups() {
        return myGroups;
    }

    @Exclude
    public ArrayList<Group.Info> getGroupsAsList() {
        ArrayList<Group.Info> list = new ArrayList<>(myGroups.values());
        list.sort(null);
        return list;
    }
    public void addGroup(Group.Info g) {
        this.myGroups.put(g.getId(), g);
    }

    public Info getInfo() {
        return info;
    }

    public static class Info implements Serializable {
        public static final float serialVersionUID = 1;

        private String id;
        private String name;

        public Info() {
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
