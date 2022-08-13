package com.unipi.sam.getnotes.table;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;
import com.unipi.sam.getnotes.LocalDatabase;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class Group {
    private Info info = new Info();
    private ArrayList<User.Info> members = new ArrayList<>();
    private HashMap<String, ArrayList<Object>> storage = new HashMap<>();
    private enum OBJECTS {
        NOTE,
        CONCEPT
    }

    public Group() {
    }

    public Group(String authorId, String authorName, String name) {
        this.info.id = UUID.randomUUID().toString();
        this.info.authorId = authorId;
        this.info.authorName = authorName;
        this.info.groupName = name;
        this.info.snapDate();
    }

    @Exclude
    public String getId() {
        return info.id;
    }

    @Exclude
    public String getAuthorId() {
        return info.authorId;
    }

    @Exclude
    public String getAuthorName() {
        return info.authorName;
    }

    @Exclude
    public String getGroupName() {
        return info.groupName;
    }

    public Info getInfo() {
        return info;
    }

    public HashMap<String, ArrayList<Object>> getStorage() {
        if(storage.isEmpty()) {
            storage.put(String.valueOf(0), new ArrayList<>());
        }
        return storage;
    }

    public void setStorage(HashMap<String, ArrayList<Object>> storage) {
        this.storage = storage;
    }

    public void addConcept(Concept c) {
        addConcept(c, "root");
    }

    public void addConcept(Concept c, String parentFolder) {
        storage.putIfAbsent(parentFolder, new ArrayList<>());
        storage.get(parentFolder).add(c);
    }

    public ArrayList<User.Info> getMembers() {
        return members;
    }

    public static class Concept implements Serializable{
        private String id;
        private String name;
        public static final int TYPE_CONCEPT = 1;
        private OBJECTS type = OBJECTS.CONCEPT;

        public Concept() {}

        public Concept(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Concept(String name) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public OBJECTS getType() {
            return type;
        }

        public static Concept fromMap(HashMap<String, Object> map) {
            if(!Objects.equals(map.get("type"), "CONCEPT")) return null;

            Concept c = new Concept();
            c.id = (String) map.get("id");
            c.name = (String) map.get("name");
            return c;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Concept concept = (Concept) o;
            return id.equals(concept.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Concept{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    public static class Note implements Serializable{
        private String id;
        private String name;
        private OBJECTS type = OBJECTS.NOTE;

        public Note() {
        }

        public Note(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Note(String noteName) {
            this.id = UUID.randomUUID().toString();
            this.name = noteName;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public OBJECTS getType() {
            return type;
        }

        public static Note fromMap(HashMap<String, Object> map) {
            Note n = new Note();
            n.id = (String) map.get("id");
            n.name = (String) map.get("name");
            return n;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Note note = (Note) o;
            return Objects.equals(id, note.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    public static class Info implements Comparable<Info>, Serializable {
        private String id;
        private String authorId;
        private String authorName;
        private String groupName;
        private String date;
        private final transient SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

        public Info() {

        }

        public String getId() {
            return id;
        }

        public String getAuthorId() {
            return authorId;
        }

        public String getAuthorName() {
            return authorName;
        }

        public String getGroupName() {
            return groupName;
        }

        public String getDate() {
            return date;
        }

        public void snapDate() {
            this.date = sdf.format(new Date());
        }

        @Override
        public int compareTo(Info i) {
            try {
                Date d1 = sdf.parse(date);
                Date d2 = sdf.parse(i.date);

                Objects.requireNonNull(d1);
                Objects.requireNonNull(d2);
                return -d1.compareTo(d2);
            } catch (ParseException | NullPointerException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Group{" +
                "groupName='" + info.groupName + '\'' +
                ", authorName='" + info.authorName + '\'' +
                '}';
    }
}
