package com.unipi.sam.getnotes.utility;

import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.unipi.sam.getnotes.note.BlackboardFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

public class CachedList<T> implements RemovalListener<Integer, T>, Iterable<T> {
    public interface Factory<T> {
        Serializable getSerializable(T e);

        T getElement(Serializable e);
    }

    private File cacheDir;
    private File categoryDir;
    private LoadingCache<Integer, T> cache;
    private final CacheBuilder<Integer, T> builder = CacheBuilder.newBuilder()
            .removalListener(this);
    private int items = 0;
    private Factory<T> factory;

    public CachedList(int maxCapacity) {
        initCache(maxCapacity);
    }

    public CachedList(int maxCapacity, File cacheDir, Factory<T> factory) {
        setCacheDirectory(cacheDir);
        this.factory = factory;
        initCache(maxCapacity);
    }

    public CachedList(int maxCapacity, Factory<T> factory) {
        this.factory = factory;
        initCache(maxCapacity);
    }

    private void initCache(int maxCapacity) {
        builder.maximumSize(maxCapacity);
        cache = builder.build(new CacheLoader<Integer, T>() {
            @Override
            public T load(Integer integer) {
                return read(integer);
            }
        });
    }

    public int size() {
        return items;
    }

    public void refresh(int from, int to) {
        if(from == to)
            refresh(from);

        for (int i = from; i < to; i++) {
            refresh(i);
        }
    }

    public int internalSize() {
        return (int) cache.size();
    }

    public LoadingCache<Integer, T> getCache() {
        return cache;
    }

    public void setCacheDirectory(File cacheDir) {
        this.cacheDir = (cacheDir == null ? new File(Environment.getExternalStorageDirectory() + File.separator + "cache-list") : cacheDir);

        if (!this.cacheDir.exists()) {
            try {
                boolean created = this.cacheDir.mkdirs();
                if (!created) throw new FileNotFoundException("Cannot create: " + cacheDir.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setCategory(String category) {
        if (cacheDir == null) {
            cacheDir = new File(Environment.getExternalStorageDirectory() + File.separator + category);
            setCacheDirectory(cacheDir);
            return;
        }

        categoryDir = new File(cacheDir, category);
        boolean created = categoryDir.mkdir();

        if (!created) try {
            throw new FileNotFoundException("Cannot create file");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int add(T t) {
        cache.put(items, t);
        write(items, t);

        items++;
        return items - 1;
    }

    public void update(int index, T t) {
        write(index, t);
    }

    public void refresh(int currentPage) {
        cache.refresh(currentPage);
    }

    public T get(int index) {
        try {
            return cache.get(index, ()-> read(index));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void evictAll() {
        cache.invalidateAll();
    }

    private void write(int index, T t) {
        try {
            Serializable element;
            if (!(t instanceof Serializable) && factory != null) {
                element = factory.getSerializable(t);
            } else if (!(t instanceof Serializable)) {
                throw new NotSerializableException("element is not serializable and no factory is provided");
            } else element = (Serializable) t;

            File file = new File(getDir(), String.valueOf(index));
            FileOutputStream fout = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fout);
            out.writeObject(element);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private T read(int index) {
        try {
            File file = new File(getDir(), String.valueOf(index));
            FileInputStream fin = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fin);
            Object o = in.readObject();
            in.close();

            if(o != null && items <= index)
                items = index + 1;

            if (factory != null) {
                return factory.getElement((Serializable) o);
            }

            return (T) o;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private File getDir() {
        return categoryDir == null ? cacheDir : categoryDir;
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return new CachedIterator(0);
    }

    public Iterator<T> iterator(int index) {
        return new CachedIterator(index);
    }

    @NonNull
    @Override
    public String toString() {
        if (cache.size() == 0) return "In-Memory: []";

        Iterator<T> iterator = iterator();
        StringBuilder buffer = new StringBuilder("[");

        while (iterator.hasNext()) {
            buffer.append(iterator.next()).append(", ");
        }
        buffer.deleteCharAt(buffer.length() - 1);
        buffer.deleteCharAt(buffer.length() - 1);
        buffer.append("]");
        return "In-Memory: " + buffer;
    }

    @Override
    public void onRemoval(RemovalNotification<Integer, T> rn) {
        if (rn.getKey() == null) return;

        write(rn.getKey(), rn.getValue());
    }

    private class CachedIterator implements Iterator<T> {
        private int index;
        private final int currentItems;

        public CachedIterator(int index) {
            this.index = index;
            this.currentItems = items;
        }

        @Override
        public boolean hasNext() {
            return index < currentItems;
        }

        @Override
        public T next() {
            if (currentItems != items)
                throw new ConcurrentModificationException("list changed during loop");
            return get(index++);
        }
    }
}
