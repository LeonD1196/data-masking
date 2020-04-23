import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
//        shrink();
//        normaliseData();
//        test();
        double k = 0;
        ArrayList<String> disclosureRisk = new ArrayList<>();
        ArrayList<String> informationLoss = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("0.0000000000000000000000000000000000");

        for (int i = 0; i < 5; i++) {
            String fileName = "k-" + k + "-masked.xls";
            maskData(k, fileName);
            disclosureRisk.add(df.format(getDisclosureRisk(fileName)));
            informationLoss.add(df.format(getInformationLoss(fileName)));
            k += 0.4;
        }

        System.out.println("DR: " + disclosureRisk + "\nIL: " + informationLoss);
    }

    private static double getDisclosureRisk(String fileName) {
        HashMap<Integer, ArrayList<Double>> originalData = getRowData("normalised-original-data.xls");
        HashMap<Integer, ArrayList<Double>> maskedData = getRowData(fileName);

        int reIdentified = 0;
        for (int i = 1; i <= originalData.size(); i++) {
            int minDistance = Integer.MAX_VALUE;
            int minIndex = -1;

            for (int j = 1; j <= maskedData.size(); j++) {
                int currentDistance = getEuclideanDistance(originalData.get(i), maskedData.get(j));
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    minIndex = j;
                }
            }

            if (minIndex == i)
                reIdentified++;
        }

        return reIdentified;

    }

    private static double getInformationLoss(String fileName) {
        HashMap<Integer, ArrayList<Double>> originalData = getColData("normalised-original-data.xls");
        HashMap<Integer, ArrayList<Double>> maskedData = getColData(fileName);

        double sum = 0;

        for (int i = 1; i <= originalData.size(); i++)
            sum += getMeanSquareError(originalData.get(i), maskedData.get(i));

        return (sum / (double) 14040) * 100;
    }

    private static int getMeanSquareError(ArrayList<Double> originalData, ArrayList<Double> maskedData) {
            double sum = 0;
            for (int i = 0; i < originalData.size(); i++) {
                    sum += Math.pow((originalData.get(i) - maskedData.get(i)), 2.0);
            }
            return (int) sum / (originalData.size());
    }

    private static int getEuclideanDistance(ArrayList<Double> originalData, ArrayList<Double> maskedData) {
        double sum = 0;

        for (int i = 0; i < originalData.size(); i++)
            sum += Math.pow((originalData.get(i) - maskedData.get(i)), 2.0);

        return (int) Math.sqrt(sum);
    }

    private static HashMap<Integer, ArrayList<Double>> getColData(String fileName) {
        HashMap<Integer, ArrayList<Double>> data = new HashMap<>();
        try (InputStream inp = new FileInputStream(fileName)) {
            Workbook wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            int index = 1;

            for (int i = 0; i < 13; i++) {
                data.put(index, getColumnedData(sheet, i));
                index++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    private static HashMap<Integer, ArrayList<Double>> getRowData(String fileName) {
        HashMap<Integer, ArrayList<Double>> data = new HashMap<>();
        try (InputStream inp = new FileInputStream(fileName)) {
            Workbook wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            int index = 1;

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;

                ArrayList<Double> rowData = getRowData(sheet, index);
                data.put(index, rowData);
                index++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    private static ArrayList<Double> getRowData(Sheet sheet, int index) {
        ArrayList<Double> data = new ArrayList<>();
        for (Cell cell : sheet.getRow(index))
            data.add(cell.getNumericCellValue());
        return data;
    }

    private static ArrayList<Double> getColumnedData(Sheet sheet, int cellIndex) {
        ArrayList<Double> values = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0)
                continue;

            values.add(row.getCell(cellIndex).getNumericCellValue());
        }

        return values;
    }

    private static double getMean(ArrayList<Double> data) {
        double sum = 0;

        for (Double value : data) {
            sum += value;
        }
        return sum / (double) data.size();
    }

    private static double getStandardDeviation(ArrayList<Double> data, double mean) {
        ArrayList<Double> updatedData = new ArrayList<>();
        for (Double value : data) {
            double valueCopy = value;
            valueCopy -= mean;
            valueCopy *= valueCopy;
            updatedData.add(valueCopy);
        }
        double updatedMean = getMean(updatedData);
        return Math.sqrt(updatedMean);
    }

    private static double getVariance(ArrayList<Double> data, double mean) {
        ArrayList<Double> updatedData = new ArrayList<>();
        for (Double value : data) {
            double valueCopy = value;
            valueCopy -= mean;
            valueCopy *= valueCopy;
            updatedData.add(valueCopy);
        }
        return getMean(updatedData);
    }

    private static double getNormalisedValue(double value, double mean, double standardDeviation) {
        return (value - mean) / standardDeviation;
    }

    private static void shrink() {
        try (InputStream inp = new FileInputStream("original-data.xls")) {
            Workbook wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);

            for (int i = 0; i < 13; i++) {
                for (Row row : sheet) {
                    if (row.getRowNum() == 0)
                        continue;

                    Cell cell = row.getCell(i);
                    double value = cell.getNumericCellValue();
                    cell.setCellValue(value / 1000);
                }

                try (OutputStream filOut = new FileOutputStream("shrunk-original-data.xls")) {
                    wb.write(filOut);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void test() {
        try (InputStream inp = new FileInputStream("normalised-original-data.xls")) {
            Workbook wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            ArrayList<Double> data;
            double mean, variance, standardDeviation;

            for (int i = 0; i < 13; i++) {
                data = getColumnedData(sheet, i);
                mean = getMean(data);
                variance = getVariance(data, mean);
                standardDeviation = getStandardDeviation(data, mean);
                System.out.println("Column: " + (i + 1));
                System.out.println("Mean: " + mean);
                System.out.println("Variance: " + variance);
                System.out.println("SD: " + standardDeviation);
                System.out.println();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void normaliseData() {
        try (InputStream inp = new FileInputStream("shrunk-original-data.xls")) {
            Workbook wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            ArrayList<Double> data = new ArrayList<>();
            double mean, standardDeviation;

            for (int i = 0; i < 13; i++) {
                data.clear();

                data = getColumnedData(sheet, i);
                mean = getMean(data);
                standardDeviation = getStandardDeviation(data, mean);

                for (Row row : sheet) {
                    if (row.getRowNum() == 0)
                        continue;

                    double value = data.get(0);
                    double normalisedValue = getNormalisedValue(value, mean, standardDeviation);

                    data.remove(0);

                    Cell cell = row.getCell(i);
                    cell.setCellValue(normalisedValue);
                }

                try (OutputStream filOut = new FileOutputStream("normalised-original-data.xls")) {
                    wb.write(filOut);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void maskData(double k, String newFileName) {
        try (InputStream inp = new FileInputStream("normalised-original-data.xls")) {
            Workbook wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            ArrayList<Double> data = new ArrayList<>();
            double mean, variance;

            for (int i = 0; i < 13; i++) {
                data.clear();

                data = getColumnedData(sheet, i);
                mean = getMean(data);
                variance = getVariance(data, mean);

                for (Row row : sheet) {
                    if (row.getRowNum() == 0)
                        continue;

                    double value = data.get(0);
                    data.remove(0);

                    Cell cell = row.getCell(i);

                    double maskedData = value + new Random().nextGaussian() * Math.sqrt(variance + k);
                    cell.setCellValue(maskedData);
                }

                try (OutputStream filOut = new FileOutputStream(newFileName)) {
                    wb.write(filOut);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
