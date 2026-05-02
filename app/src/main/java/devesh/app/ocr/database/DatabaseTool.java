package devesh.app.ocr.database;

import android.content.Context;

import androidx.room.Room;

import java.util.List;

public class DatabaseTool {

    AppDatabase db;
    UserDao userDao;

    public DatabaseTool(Context context){
         db = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, "ocrdb").allowMainThreadQueries().fallbackToDestructiveMigration().build();
         userDao = db.userDao();

    }

    public List<ScanFile> getAll(){
        return userDao.getAll();
    }

    public void Add(ScanFile scanFile){
        userDao.insert(scanFile);
    }

    public void update(ScanFile scanFile){
        userDao.update(scanFile);
    }

    public ScanFile findByText(String text){
        return userDao.findByText(text);
    }

    public void delete(ScanFile scanFile) {
        userDao.delete(scanFile);
    }

    public void clearHistory(){
        userDao.nukeTable();
    }



}
