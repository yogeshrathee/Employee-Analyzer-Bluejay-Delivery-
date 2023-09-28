package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class EmployeeAnalyzer {
    public static void main(String[] args) {
        String excelFilePath = "employee_data.xlsx"; // Update with your file path

        try (FileInputStream fis = new FileInputStream(new File(excelFilePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Assuming the data is in the first sheet
            Iterator<Row> rowIterator = sheet.iterator();

            Map<String, List<Shift>> employeeShifts = new HashMap<>();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() == 0) continue; // Skip the header row

                String employeeName = row.getCell(7).getStringCellValue(); // Assuming the employee name is in column H (index 7)

                // Check if the cell is formatted as a date
                Cell startTimeCell = row.getCell(2);
                if (startTimeCell.getCellType() != CellType.NUMERIC) {
                    // Skip this row silently without printing a message
                    continue;
                }

                Date startTime = startTimeCell.getDateCellValue();

                // Check if the cell is formatted as a date
                Cell endTimeCell = row.getCell(3);
                if (endTimeCell.getCellType() != CellType.NUMERIC) {
                    // Skip this row silently without printing a message
                    continue;
                }

                Date endTime = endTimeCell.getDateCellValue();

                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                String currentDate = dateFormat.format(startTime);

                List<Shift> shifts = employeeShifts.getOrDefault(employeeName, new ArrayList<>());
                Shift shift = new Shift(startTime, endTime, currentDate);

                shifts.add(shift);
                employeeShifts.put(employeeName, shifts);
            }

            for (Map.Entry<String, List<Shift>> entry : employeeShifts.entrySet()) {
                String employeeName = entry.getKey();
                List<Shift> shifts = entry.getValue();
                analyzeShifts(employeeName, shifts);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void analyzeShifts(String employeeName, List<Shift> shifts) {
        int consecutiveDaysCount = checkConsecutiveDays(shifts);
        int hoursBetweenShiftsCount = checkHoursBetweenShifts(shifts);
        int longShiftsCount = checkLongShift(shifts);

        if (consecutiveDaysCount > 0 || hoursBetweenShiftsCount > 0 || longShiftsCount > 0) {
            System.out.println("Employee Name: " + employeeName);
            System.out.println("Conditions met:");
            if (consecutiveDaysCount > 0) {
                System.out.println("- Worked for " + consecutiveDaysCount + " sets of 7 consecutive days");
            }
            if (hoursBetweenShiftsCount > 0) {
                System.out.println("- " + hoursBetweenShiftsCount + " shifts with less than 10 hours but greater than 1 hour between them");
            }
            if (longShiftsCount > 0) {
                System.out.println("- " + longShiftsCount + " shifts exceeding 14 hours");
            }
            System.out.println();
        }

    }

    private static int checkConsecutiveDays(List<Shift> shifts) {
        int consecutiveSets = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < shifts.size() - 6; i++) {
            Date currentDate = shifts.get(i).startTime;
            calendar.setTime(currentDate);

            boolean consecutive = true;
            for (int j = 1; j < 7; j++) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                Date nextDate = calendar.getTime();
                if (!shifts.get(i + j).date.equals(dateFormat.format(nextDate))) {
                    consecutive = false;
                    break;
                }
            }

            if (consecutive) {
                consecutiveSets++;
            }
        }

        return consecutiveSets;
    }

    private static int checkHoursBetweenShifts(List<Shift> shifts) {
        int shiftsMeetingCriteria = 0;
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        for (int i = 0; i < shifts.size() - 1; i++) {
            Date currentEndTime = shifts.get(i).endTime;
            Date nextStartTime = shifts.get(i + 1).startTime;

            long durationInMillis = nextStartTime.getTime() - currentEndTime.getTime();
            long hoursBetween = durationInMillis / (60 * 60 * 1000); // Convert milliseconds to hours

            if (hoursBetween > 1 && hoursBetween < 10) {
                shiftsMeetingCriteria++;
            }
        }

        return shiftsMeetingCriteria;
    }

    private static int checkLongShift(List<Shift> shifts) {
        int longShifts = 0;

        for (Shift shift : shifts) {
            long durationInMillis = shift.endTime.getTime() - shift.startTime.getTime();
            long hoursWorked = durationInMillis / (60 * 60 * 1000); // Convert milliseconds to hours
            if (hoursWorked > 14) {
                longShifts++;
            }
        }

        return longShifts;
    }

    private static class Shift {
        private final Date startTime;
        private final Date endTime;
        private final String date;

        public Shift(Date startTime, Date endTime, String date) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.date = date;
        }
    }
}
