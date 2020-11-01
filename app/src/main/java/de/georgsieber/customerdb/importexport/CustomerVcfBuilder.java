package de.georgsieber.customerdb.importexport;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.georgsieber.customerdb.model.Customer;

public class CustomerVcfBuilder {

    static class VcfEntry {
        ArrayList<VcfField> mFields = new ArrayList<>();
        VcfEntry() {
            super();
        }
    }
    static class VcfField {
        String[] mOptions;
        String[] mValues;
        VcfField(String[] options, String[] values) {
            mOptions = options;
            mValues = values;
        }
    }

    private List<Customer> mCustomers = new ArrayList<>();

    public CustomerVcfBuilder(List<Customer> _customers) {
        mCustomers = _customers;
    }
    public CustomerVcfBuilder(Customer _customer) {
        mCustomers.add(_customer);
    }

    @SuppressLint("SimpleDateFormat")
    private static DateFormat formatWithoutDashes = new SimpleDateFormat("yyyyMMdd");

    private String buildVcfContent() {
        StringBuilder content = new StringBuilder();
        for(Customer currentCustomer : mCustomers) {
            content.append("BEGIN:VCARD\n");
            content.append("VERSION:2.1\n");
            content.append("FN;ENCODING=QUOTED-PRINTABLE:").append(QuotedPrintable.encode(currentCustomer.mTitle)).append(" ").append(QuotedPrintable.encode(currentCustomer.mFirstName)).append(" ").append(QuotedPrintable.encode(currentCustomer.mLastName)).append("\n");
            content.append("N;ENCODING=QUOTED-PRINTABLE:").append(QuotedPrintable.encode(currentCustomer.mLastName)).append(";").append(QuotedPrintable.encode(currentCustomer.mFirstName)).append(";;").append(QuotedPrintable.encode(currentCustomer.mTitle)).append(";\n");
            if(!currentCustomer.mPhoneHome.equals(""))
                content.append("TEL;HOME:").append(escapeVcfValue(currentCustomer.mPhoneHome)).append("\n");
            if(!currentCustomer.mPhoneMobile.equals(""))
                content.append("TEL;CELL:").append(escapeVcfValue(currentCustomer.mPhoneMobile)).append("\n");
            if(!currentCustomer.mPhoneWork.equals(""))
                content.append("TEL;WORK:").append(escapeVcfValue(currentCustomer.mPhoneWork)).append("\n");
            if(!currentCustomer.mEmail.equals(""))
                content.append("EMAIL;INTERNET:").append(currentCustomer.mEmail).append("\n");
            if(!currentCustomer.getAddress().equals(""))
                content.append("ADR;TYPE=HOME:" + ";;").append(escapeVcfValue(currentCustomer.mStreet)).append(";").append(escapeVcfValue(currentCustomer.mCity)).append(";;").append(escapeVcfValue(currentCustomer.mZipcode)).append(";").append(escapeVcfValue(currentCustomer.mCountry)).append("\n");
            if(!currentCustomer.mCustomerGroup.equals(""))
                content.append("ORG:").append(escapeVcfValue(currentCustomer.mCustomerGroup)).append("\n");
            if(currentCustomer.mBirthday != null)
                content.append("BDAY:").append(formatWithoutDashes.format(currentCustomer.mBirthday)).append("\n");
            if(!currentCustomer.mNotes.equals(""))
                content.append("NOTE;ENCODING=QUOTED-PRINTABLE:").append(QuotedPrintable.encode(currentCustomer.mNotes)).append("\n");
            if(!currentCustomer.mCustomFields.equals(""))
                content.append("X-CUSTOM-FIELDS:").append(escapeVcfValue(currentCustomer.mCustomFields)).append("\n");
            if(currentCustomer.mImage != null && currentCustomer.mImage.length > 0) {
                String base64String = Base64.encodeToString(currentCustomer.mImage, Base64.DEFAULT);
                base64String = base64String.replace("\n", "\n ").trim();
                content.append("PHOTO;ENCODING=BASE64;JPEG:").append(base64String).append("\n");
            }
            content.append("END:VCARD\n\n");
        }

        return content.toString();
    }

    private String escapeVcfValue(String value) {
        return value.replace("\n", "\\n");
    }

    public boolean saveVcfFile(File f) {
        try {
            FileOutputStream stream = new FileOutputStream(f);
            stream.write(buildVcfContent().getBytes());
            stream.close();
            return true;
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        return false;
    }

    private static String[] VCF_FIELDS = {
            // thanks, Wikipedia
            "ADR", "AGENT", "ANNIVERSARY",
            "BDAY", "BEGIN", "BIRTHPLACE",
            "CALADRURI", "CALURI", "CATEGORIES", "CLASS", "CLIENTPIDMAP",
            "DEATHDATE", "DEATHPLACE",
            "EMAIL", "END", "EXPERTISE",
            "FBURL", "FN",
            "GENDER", "GEO",
            "HOBBY",
            "IMPP", "INTEREST",
            "KEY", "KIND",
            "LABEL", "LANG", "LOGO",
            "MAILER", "MEMBER",
            "N", "NAME", "NICKNAME", "NOTE",
            "ORG", "ORG-DIRECTORY",
            "PHOTO", "PROID", "PROFILE",
            "RELATED", "REV", "ROLE",
            "SORT-STRING", "SOUND", "SOURCE",
            "TEL", "TITLE", "TZ",
            "UID", "URL",
            "VERSION",
            "XML"
    };
    private static boolean isVcfField(String text) {
        String upperText = text.toUpperCase();
        String[] keyValue = upperText.split(":");
        if(keyValue.length >= 1) {
            String[] subKeys = keyValue[0].split(";");
            if(subKeys[0].startsWith("X-")) {
                return true;
            }
            for(String field : VCF_FIELDS) {
                if(subKeys[0].startsWith(field)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("StringConcatenationInLoop")
    public static List<Customer> readVcfFile(InputStreamReader isr) throws IOException {
        // STEP 0 : a vcf field can be broken up into multiple lines
        // we concatenate them here into a single line again
        BufferedReader br = new BufferedReader(isr);
        ArrayList<String> vcfFields = new ArrayList<>();
        String currFieldValue = "";
        String line0;
        while((line0 = br.readLine()) != null) {
            if(isVcfField(line0)) {
                if(!currFieldValue.trim().equals("")) {
                    vcfFields.add(currFieldValue);
                    //Log.e("VCF FIELD", currFieldValue);
                }
                currFieldValue = line0.trim();
            } else {
                String append = line0.trim();
                // avoid the double equal sign hell
                if(append.startsWith("=") && currFieldValue.endsWith("=")) {
                    append = append.substring(1);
                }
                currFieldValue += append;
            }
        }
        if(!currFieldValue.trim().equals("")) {
            vcfFields.add(currFieldValue);
            //Log.e("VCF FIELD", currFieldValue);
        }

        // STEP 1 : parse VCF string into structured data
        VcfEntry tempVcfEntry = null;
        ArrayList<VcfEntry> tempVcfEntries = new ArrayList<>();
        for(String line : vcfFields) {
            String upperLine = line.toUpperCase();
            if(upperLine.startsWith("BEGIN:VCARD")) {
                tempVcfEntry = new VcfEntry();
            }
            if(upperLine.startsWith("END:VCARD")) {
                if(tempVcfEntry != null) {
                    tempVcfEntries.add(tempVcfEntry);
                }
                tempVcfEntry = null;
            }
            if(tempVcfEntry != null) {
                String[] keyValue = line.split(":", 2);
                if(keyValue.length != 2) continue;
                String[] options = keyValue[0].split(";");
                String[] values = keyValue[1].split(";");
                if(QuotedPrintable.isVcfFieldQuotedPrintableEncoded(options)) {
                    // decode quoted printable encoded fields
                    ArrayList<String> decodedValuesList = new ArrayList<>();
                    for (String v : values) {
                        decodedValuesList.add(QuotedPrintable.decode(v));
                    }
                    tempVcfEntry.mFields.add(new VcfField(options, decodedValuesList.toArray(new String[0])));
                } else {
                    tempVcfEntry.mFields.add(new VcfField(options, values));
                }
            }
        }

        // STEP 2 : create customers from VCF data
        List<Customer> newCustomers = new ArrayList<>();
        for(VcfEntry e : tempVcfEntries) {
            String fullNameTemp = "";
            Customer newCustomer = new Customer();

            // apply all VCF fields
            for(VcfField f : e.mFields) {
                switch(f.mOptions[0].toUpperCase()) {
                    case "FN":
                        if(f.mValues.length >= 1) fullNameTemp = f.mValues[0];
                        break;

                    case "N":
                        if(f.mValues.length >= 1) newCustomer.mLastName = f.mValues[0];
                        if(f.mValues.length >= 2) newCustomer.mFirstName = f.mValues[1];
                        if(f.mValues.length >= 4) newCustomer.mTitle = f.mValues[3];
                        break;

                    case "EMAIL":
                        if(f.mValues.length >= 1) newCustomer.mEmail = f.mValues[0];
                        break;

                    case "ADR":
                        String street = "";
                        String zipcode = "";
                        String city = "";
                        String country = "";
                        if(f.mValues.length > 2) street = f.mValues[2];
                        if(f.mValues.length > 3) city = f.mValues[3];
                        if(f.mValues.length > 5) zipcode = f.mValues[5];
                        if(f.mValues.length > 6) country = f.mValues[6];
                        if(newCustomer.mStreet.trim().equals("") && newCustomer.mZipcode.trim().equals("") && newCustomer.mCity.trim().equals("") && newCustomer.mCountry.trim().equals("")) {
                            newCustomer.mStreet = street;
                            newCustomer.mZipcode = zipcode;
                            newCustomer.mCity = city;
                            newCustomer.mCountry = country;
                        } else {
                            addAdditionalInfoToDescription(newCustomer, street + "\n" + zipcode + " " + city + "\n" + country);
                        }
                        break;

                    case "TITLE":
                    case "URL":
                    case "NOTE":
                        addAdditionalInfoToDescription(newCustomer, f.mValues[0]);
                        break;

                    case "X-CUSTOM-FIELDS":
                        newCustomer.mCustomFields = f.mValues[0];
                        break;

                    case "TEL":
                        String telParam = "";
                        if(f.mOptions.length > 1) telParam = f.mOptions[1].trim();
                        addTelephoneNumber(newCustomer, telParam, f.mValues[0]);
                        break;

                    case "ORG":
                        newCustomer.mCustomerGroup = f.mValues[0];
                        break;

                    case "BDAY":
                        try {
                            newCustomer.mBirthday = formatWithoutDashes.parse(f.mValues[0]);
                        } catch (Exception ignored) {}
                        break;

                    case "PHOTO":
                        newCustomer.putCustomerAttribute("image", f.mValues[0]);
                        break;
                }
            }

            // apply name fallback if name is empty
            if(newCustomer.mFirstName.equals("") && newCustomer.mLastName.equals("")) {
                newCustomer.mLastName = fullNameTemp;
            }

            // only add if name is not empty
            if(!(newCustomer.mFirstName.equals("") && newCustomer.mLastName.equals("") && newCustomer.mTitle.equals(""))) {
                newCustomers.add(newCustomer);
            }
        }

        return newCustomers;
    }

    private static void addTelephoneNumber(Customer currentCustomer, String telParam, String telValue) {
        String telParamUpper = telParam.toUpperCase();

        if(telParamUpper.startsWith("HOME")) {

            if(currentCustomer.mPhoneHome.trim().equals(""))
                currentCustomer.mPhoneHome = telValue;
            else
                addTelephoneNumberAlternative(currentCustomer, telParam, telValue);

        } else if(telParamUpper.startsWith("CELL")) {

            if(currentCustomer.mPhoneMobile.trim().equals(""))
                currentCustomer.mPhoneMobile = telValue;
            else
                addTelephoneNumberAlternative(currentCustomer, telParam, telValue);

        } else if(telParamUpper.startsWith("WORK")) {

            if(currentCustomer.mPhoneWork.trim().equals(""))
                currentCustomer.mPhoneWork = telValue;
            else
                addTelephoneNumberAlternative(currentCustomer, telParam, telValue);

        } else {

            if(currentCustomer.mPhoneHome.equals("")) {
                currentCustomer.mPhoneHome = telValue;
            } else if(currentCustomer.mPhoneMobile.equals("")) {
                currentCustomer.mPhoneMobile = telValue;
            } else if(currentCustomer.mPhoneWork.equals("")) {
                currentCustomer.mPhoneWork = telValue;
            } else {
                addTelephoneNumberAlternative(currentCustomer, telParam, telValue);
            }

        }
    }
    private static void addTelephoneNumberAlternative(Customer currentCustomer, String telParam, String telValue) {
        addAdditionalInfoToDescription(currentCustomer, telParam + ": " + telValue);
    }
    private static void addAdditionalInfoToDescription(Customer currentCustomer, String info) {
        currentCustomer.mNotes = (currentCustomer.mNotes + "\n\n" + info).trim();
    }
}
