package com.dominity.ing.atm.service;

import com.dominity.ing.utils.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

public class ATMService {
    private static final Comparator<Task> COMPARATOR = (c1, c2) -> c1.getRegionNo() != c2.getRegionNo()
            ? c1.getRegionNo() - c2.getRegionNo() : (c1.getRequestType().getPriority() == c2.getRequestType().getPriority()
            ? (c1.getAtmId() == c2.getAtmId() ? 0 : 1) : c1.getRequestType().getPriority() - c2.getRequestType().getPriority());

    public void prepareRoute(InputStream inputStream, OutputStream outputStream) throws IOException {
        JsonParser.scrollStreamTill('[', inputStream);
        var tasks = parseTasks(inputStream);
        prepareResponse(tasks, outputStream);
    }

    private Collection<Task> parseTasks(InputStream inputStream) throws IOException {
        //TODO validate what it takes to parse payload only without sorting
        //TODO compare to performance of ObjectMapper
        var tasks = new TreeSet<>(COMPARATOR);
        byte ch;
        while ((ch = (byte) inputStream.read()) != -1) {
            if (ch != '{') {
                continue;
            }
            tasks.add(parseTask(inputStream));
        }
        return tasks;
    }

    private Task parseTask(InputStream inputStream) throws IOException {
        var task = new Task();

        parseJsonField(task, inputStream);
        parseJsonField(task, inputStream);
        parseJsonField(task, inputStream);

        if (task.getRegionNo() == 0 || task.getAtmId() == 0 || task.getRequestType() == null) {
            throw new IllegalArgumentException("Unparseable json");
        }
        return task;
    }

    private void prepareResponse(Collection<Task> tasks, OutputStream outputStream) throws IOException {
        if (tasks.isEmpty()) {
            outputStream.write('[');
            outputStream.write(']');
            return;
        }
        outputStream.write('[');
        var iterator = tasks.iterator();
        var previousTask = iterator.next();
        while (iterator.hasNext()) {
            var currentTask = iterator.next();
            if (!areTasksForSameAtm(previousTask, currentTask)) {
                writeTask(outputStream, previousTask);
                outputStream.write(',');
            }
            previousTask = currentTask;
        }
        writeTask(outputStream, previousTask);
        outputStream.write(']');
    }

    private boolean areTasksForSameAtm(Task toCheck, Task fromCollection) {
        return toCheck.getRegionNo() == fromCollection.getRegionNo() && toCheck.getAtmId() == fromCollection.getAtmId();
    }

    private void writeTask(OutputStream outputStream, Task previousTask) throws IOException {
        outputStream.write('{');
        writeFieldName(FieldName.REGION.getName(), outputStream);
        outputStream.write(':');
        writeInt(previousTask.getRegionNo(), outputStream);
        outputStream.write(',');
        writeFieldName(FieldName.ATM_ID.getName(), outputStream);
        outputStream.write(':');
        writeInt(previousTask.getAtmId(), outputStream);
        outputStream.write('}');
    }

    private void writeFieldName(String fieldName, OutputStream outputStream) throws IOException {
        outputStream.write('"');
        for (int i = 0; i < fieldName.length(); i ++) {
            outputStream.write(fieldName.charAt(i));
        }
        outputStream.write('"');
    }

    private void writeInt(int value, OutputStream outputStream) throws IOException {
        var valueStr = Integer.toString(value);
        for (int i = 0; i < valueStr.length(); i ++) {
            outputStream.write(valueStr.charAt(i));
        }
    }

    private void parseJsonField(Task task, InputStream inputStream) throws IOException {
        JsonParser.scrollStreamTill('"', inputStream);
        var fieldName = parseFieldName(inputStream);
        if (FieldName.REQUEST_TYPE == fieldName) {
            JsonParser.scrollStreamTill('"', inputStream);
            task.setRequestType(parseRequestType(inputStream));
        } else if (FieldName.REGION == fieldName) {
            JsonParser.scrollStreamTill(':', inputStream);
            task.setRegionNo(JsonParser.parseInt(inputStream));
        } else if (FieldName.ATM_ID == fieldName) {
            JsonParser.scrollStreamTill(':', inputStream);
            task.setAtmId(JsonParser.parseInt(inputStream));
        }
    }

    private FieldName parseFieldName(InputStream inputStream) throws IOException {
        return JsonParser.parseFieldName(inputStream, new FieldName[FieldName.values().length], FieldName.values());
    }

    private RequestType parseRequestType(InputStream inputStream) throws IOException {
        return JsonParser.parseFieldName(inputStream, new RequestType[RequestType.values().length], RequestType.values());
    }

    private enum FieldName implements JsonParser.Named {
        REGION("region"),
        REQUEST_TYPE("requestType"),
        ATM_ID("atmId");

        private final String name;

        FieldName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum RequestType implements JsonParser.Named {
        FAILURE_RESTART(0),
        PRIORITY(1),
        SIGNAL_LOW(2),
        STANDARD(3);

        final int priority;

        RequestType(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public String getName() {
            return name();
        }
    }

    public static class Task {
        private int regionNo;
        private RequestType requestType;
        private int atmId;

        public int getRegionNo() {
            return regionNo;
        }

        public void setRegionNo(int regionNo) {
            this.regionNo = regionNo;
        }

        public RequestType getRequestType() {
            return requestType;
        }

        public void setRequestType(RequestType requestType) {
            this.requestType = requestType;
        }

        public int getAtmId() {
            return atmId;
        }

        public void setAtmId(int atmId) {
            this.atmId = atmId;
        }
    }
}
