package com.unipi.sam.getnotes.note.utility;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;

public class SerializableNote implements Serializable {
    private int id;
    private int currentPage;
    private ArrayList<Page> pages;

    public SerializableNote(int id, int nPages, int currentPage) {
        this.id = id;
        this.pages = new ArrayList<>(nPages);
        this.currentPage = currentPage;
    }

    public SerializableNote() {
        this(-1, 1, 0);
    }

    public int getCurrentPageIndex() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public ArrayList<Page> getPages() {
        return pages;
    }

    public void setPages(ArrayList<Page> pages) {
        this.pages = pages;
    }

    public void addPage(Page page) {
        this.pages.add(page);
    }

    public int getNumberOfPages() {
        return pages.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SerializableNote note = (SerializableNote) o;

        return id == note.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public static class Page implements Serializable {
        private ArrayList<Serializable> history;
        private String title;
        private String date;

        public Page() {
            this.title = "Title";
            this.history = new ArrayList<>();
        }

        public ArrayList<Serializable> getHistory() {
            return history;
        }

        public String getTitle() {
            return title;
        }

        public String getDate() {
            return date;
        }

        public void setHistory(ArrayList<Serializable> history) {
            this.history = history;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }
}
