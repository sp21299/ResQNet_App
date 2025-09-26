package com.example.resqnet_app.data;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "alerts")
public class Alert {

        @PrimaryKey(autoGenerate = true)
        public int id;

        public String title;
        public String message;
        public String timestamp;

        public Alert(String title, String message, String timestamp) {
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
        }

}
