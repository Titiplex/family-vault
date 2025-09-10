package com.titiplex.familyhub.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class BaseCrud {
    public static class Row {
        public final SimpleIntegerProperty id = new SimpleIntegerProperty();
        public final SimpleStringProperty c1 = new SimpleStringProperty();
        public final SimpleStringProperty c2 = new SimpleStringProperty();
        public final SimpleStringProperty c3 = new SimpleStringProperty();
        public final SimpleStringProperty c4 = new SimpleStringProperty();

        public Row(int id, String c1, String c2, String c3, String c4) {
            this.id.set(id);
            this.c1.set(c1);
            this.c2.set(c2);
            this.c3.set(c3);
            this.c4.set(c4);
        }

        public int getId() {
            return id.get();
        }

        public String getC1() {
            return c1.get();
        }

        public String getC2() {
            return c2.get();
        }

        public String getC3() {
            return c3.get();
        }

        public String getC4() {
            return c4.get();
        }
    }
}
