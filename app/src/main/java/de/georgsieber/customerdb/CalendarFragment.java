package de.georgsieber.customerdb;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.CustomerAppointment;
import de.georgsieber.customerdb.model.CustomerCalendar;
import de.georgsieber.customerdb.tools.DateControl;

public class CalendarFragment extends Fragment {

    private CustomerDatabase mDb;
    private Date mShowDate = new Date();
    private List<CustomerCalendar> mShowCalendars = new ArrayList<>();
    private RelativeLayout relativeLayoutCalendarRoot;

    private static final int MINUTES_IN_A_HOUR = 24 * 60;
    private static List<CustomerAppointment> mAppointments = new ArrayList<>();

    private static final int EDIT_APPOINTMENT_REQUEST = 1;

    public void show(List<CustomerCalendar> calendars, Date date) {
        mShowDate = date;
        mShowCalendars = calendars;
        drawEvents();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // init db
        mDb = new CustomerDatabase(getContext());

        // register event for redraw
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);
        relativeLayoutCalendarRoot = (RelativeLayout)v.findViewById(R.id.rl_calendar_root);
        relativeLayoutCalendarRoot.post(new Runnable() {
            @Override
            public void run() {
                drawEvents();
            }
        });
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case(EDIT_APPOINTMENT_REQUEST): {
                drawEvents();
            }
        }
    }

    private void drawEvents() {
        // clear old entries
        mAppointments.clear();
        for(int i = relativeLayoutCalendarRoot.getChildCount()-1; i >= 0; i--) {
            if(relativeLayoutCalendarRoot.getChildAt(i) instanceof CalendarAppointmentView)
                relativeLayoutCalendarRoot.removeViewAt(i);
        }

        // add new entries
        for(CustomerCalendar c : mShowCalendars) {
            for(CustomerAppointment a : mDb.getAppointments(c.mId, mShowDate, false, null)) {
                a.mColor = c.mColor;
                mAppointments.add(a);
            }
        }

        // draw entries
        if(mAppointments != null && !mAppointments.isEmpty()) {
            Collections.sort(mAppointments, new TimeComparator());
            int screenWidth = relativeLayoutCalendarRoot.getWidth();
            int screenHeight = relativeLayoutCalendarRoot.getHeight();

            List<Cluster> clusters = createClusters(createCliques(mAppointments));
            for(Cluster c : clusters) {
                for(final CustomerAppointment a : c.getAppointments()) {
                    int itemWidth = screenWidth / c.getMaxCliqueSize();
                    int leftMargin = c.getNextPosition() * itemWidth;
                    int itemHeight = Math.max(minutesToPixels(screenHeight, a.getEndTimeInMinutes()) - minutesToPixels(screenHeight, a.getStartTimeInMinutes()), (int)getResources().getDimension(R.dimen.appointment_min_height));
                    int topMargin = minutesToPixels(screenHeight, a.getStartTimeInMinutes());

                    int color = Color.LTGRAY;
                    try {
                        color = Color.parseColor(a.mColor);
                    } catch(Exception ignored) {}

                    CalendarAppointmentView appointmentView = (CalendarAppointmentView) getLayoutInflater().inflate(R.layout.item_appointment, null);
                    appointmentView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getContext(), CalendarAppointmentEditActivity.class);
                            intent.putExtra("appointment-id", a.mId);
                            startActivityForResult(intent, EDIT_APPOINTMENT_REQUEST);
                        }
                    });
                    String customerText = "";
                    if(a.mCustomerId != null) {
                        Customer relatedCustomer = mDb.getCustomerById(a.mCustomerId, false, false);
                        if(relatedCustomer != null) {
                            customerText = relatedCustomer.getFullName(false);
                        }
                    } else {
                        customerText = a.mCustomer;
                    }
                    appointmentView.setValues(a.mTitle, (customerText+"  "+a.mLocation).trim(), DateControl.displayTimeFormat.format(a.mTimeStart)+" - "+DateControl.displayTimeFormat.format(a.mTimeEnd), color);
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
                    params.setMargins(leftMargin, topMargin, 0, 0);
                    relativeLayoutCalendarRoot.addView(appointmentView, params);
                }
            }
        }
    }

    private int minutesToPixels(int screenHeight, int minutes) {
        return (screenHeight * minutes) / MINUTES_IN_A_HOUR;
    }

    public static List<Clique> createCliques(List<CustomerAppointment> appointments) {
        int startTime = appointments.get(0).getStartTimeInMinutes();
        int endTime = appointments.get(appointments.size() - 1).getEndTimeInMinutes();

        List<Clique> cliques = new ArrayList<>();

        for(int i = startTime; i <= endTime; i++) {
            Clique c = null;
            for(CustomerAppointment e : appointments) {
                if(e.getStartTimeInMinutes() < i && e.getEndTimeInMinutes() > i) {
                    if(c == null) {
                        c = new Clique();
                    }
                    c.addAppointment(e);
                }
            }
            if(c != null) {
                if(!cliques.contains(c)) {
                    cliques.add(c);
                }
            }
        }
        return cliques;
    }

    public static List<Cluster> createClusters(List<Clique> cliques) {
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster = null;
        for(Clique c : cliques) {
            if(cluster == null) {
                cluster = new Cluster();
                cluster.addClique(c);
            } else {
                if(cluster.getLastClique().intersects(c)) {
                    cluster.addClique(c);
                } else {
                    clusters.add(cluster);
                    cluster = new Cluster();
                    cluster.addClique(c);
                }
            }
        }
        if(cluster != null) {
            clusters.add(cluster);
        }
        return clusters;
    }

    public static class TimeComparator implements Comparator {
        public int compare(Object obj1, Object obj2) {
            CustomerAppointment o1 = (CustomerAppointment)obj1;
            CustomerAppointment o2 = (CustomerAppointment)obj2;
            int change1 = o1.getStartTimeInMinutes();
            int change2 = o2.getStartTimeInMinutes();
            if(change1 < change2) return -1;
            if(change1 > change2) return 1;
            int change3 = o1.getEndTimeInMinutes();
            int change4 = o2.getEndTimeInMinutes();
            if(change3 < change4) return -1;
            if(change3 > change4) return 1;
            return 0;
        }
    }

    public static class Clique {
        private List<CustomerAppointment> mAppointments = new ArrayList<>();

        public List<CustomerAppointment> getAppointments() {
            return mAppointments;
        }

        public void addAppointment(CustomerAppointment a) {
            mAppointments.add(a);
        }

        public boolean intersects(Clique clique2) {
            for(CustomerAppointment i : mAppointments) {
                for(CustomerAppointment k : clique2.mAppointments) {
                    if(i.equals(k)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static class Cluster {
        private List<Clique> cliques = new ArrayList<>();
        private int maxCliqueSize = 1;
        private int nextCurrentDrawPosition = 0;

        public void addClique(Clique c) {
            this.cliques.add(c);
            this.maxCliqueSize = Math.max(maxCliqueSize, c.getAppointments().size());
        }

        public int getMaxCliqueSize() {
            return maxCliqueSize;
        }

        public Clique getLastClique() {
            if(cliques.size() > 0){
                return cliques.get(cliques.size() - 1);
            }
            return null;
        }

        public List<CustomerAppointment> getAppointments() {
            List<CustomerAppointment> events = new ArrayList<>();
            for(Clique clique : cliques) {
                for(CustomerAppointment a : clique.getAppointments()) {
                    if(!events.contains(a)) {
                        events.add(a);
                    }
                }
            }
            return events;
        }

        public int getNextPosition() {
            int position = nextCurrentDrawPosition;
            if(position >= maxCliqueSize) {
                position = 0;
            }
            nextCurrentDrawPosition = position + 1;
            return position;
        }
    }

}
