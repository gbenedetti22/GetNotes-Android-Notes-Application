package com.unipi.sam.getnotes;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.unipi.sam.getnotes.note.utility.SerializableNote;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SaveService extends Service {
    private static final String NOTIFICATION_ID = "GetNotes_SaveService";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final HashSet<SerializableNote> set = new HashSet<>();
    private static final Object lock = new Object();
    private static SerializableNote currentSavingNote;
    private static SerializableNote emptyNote = new SerializableNote();
    private LocalDatabase localDatabase;

    public SaveService() {

    }

    @Override
    public void onCreate() {
        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_ID, NOTIFICATION_ID, NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(notificationChannel);
        Notification.Builder notBuilder = new Notification.Builder(this, NOTIFICATION_ID)
                .setContentTitle("GetNotes")
                .setContentText("Salvataggio nota...")
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        startForeground(1001, notBuilder.build());
        localDatabase = new LocalDatabase(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        executor.submit(() -> {
            while (!set.isEmpty()) {
                SerializableNote note;
                synchronized (lock) {
                    note = pollNote();
                }
                if (note == null) continue;

                commit(note);
                synchronized (lock) {
                    lock.notify();
                }

            }

            stopForeground(true);
            stopSelf();
        });

        return super.onStartCommand(intent, flags, startId);
    }

    private SerializableNote pollNote() {
        if (set.isEmpty()) return null;

        SerializableNote note = set.iterator().next();
        set.remove(note);
        return note;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void saveNote(Context context, SerializableNote note) {
        synchronized (lock) {
            set.add(note);
        }

        Intent currentIntent = new Intent(context, SaveService.class);
        context.startForegroundService(currentIntent);
    }

    public static boolean isPendingSave(int idNote) {
        synchronized (lock) {
            if (currentSavingNote != null && idNote == currentSavingNote.getId()) return true;

            emptyNote.setId(idNote);
            return set.contains(emptyNote);
        }
    }

    public static void waitFor(int id) {
        while (isPendingSave(id)) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {

                }
            }
        }
    }

    private void commit(@Nullable SerializableNote note) {
        if (note == null) return;
        try {
            currentSavingNote = note;
//            Thread.sleep(3000);
            String serializedNote = serialize(note);
            localDatabase.updateNoteContent(note.getId(), serializedNote);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            currentSavingNote = null;
        }
    }

    private String serialize(Object o) throws Exception {
        ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(arrayOut);
        out.writeObject(o);
        out.close();

        return Base64.getEncoder().encodeToString(arrayOut.toByteArray());
    }
}