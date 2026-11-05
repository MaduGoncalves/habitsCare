package com.example.habitscare;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.habitscare.dados_dao.SleepDao;
import com.example.habitscare.dados_dao.UserDao;
import com.example.habitscare.dados_dao.WalkDao;
import com.example.habitscare.dados_dao.WaterDao;
import com.example.habitscare.model.Sleep;
import com.example.habitscare.model.Walk;
import com.example.habitscare.model.Water;

@Database(entities = {Water.class, Sleep.class, Walk.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract WaterDao waterDao();
    public abstract WalkDao walkDao();
    public abstract SleepDao sleepDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase get(Context context){
        if(INSTANCE == null){
            synchronized (AppDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "habits_care_db").build();
                }
            }
        }
        return INSTANCE;
    }
}
