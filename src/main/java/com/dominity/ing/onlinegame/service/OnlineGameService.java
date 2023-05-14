package com.dominity.ing.onlinegame.service;

import com.dominity.ing.utils.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class OnlineGameService {
    private static final Comparator<Clan> COMPARATOR = (c1, c2) -> c1.points() == c2.points()
            ? (c1.numberOfPlayers() == c2.numberOfPlayers() ? 0 : c1.numberOfPlayers() - c2.numberOfPlayers())
                : c2.points() - c1.points();

    private enum TopLevelField implements JsonParser.Named {
        GROUP_COUNT("groupCount"),
        CLANS("clans");

        private final String name;

        TopLevelField(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    private enum ClanLevelField implements JsonParser.Named {
        NUMBER_OF_PLAYERS("numberOfPlayers"),
        POINTS("points");

        private final String name;

        ClanLevelField(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    private record Clan(int numberOfPlayers, int points) {}

    public void groupClans(InputStream inputStream, OutputStream outputStream) throws IOException {
        int groupCount = -1;
        var topLevelField = parseTopLevelFieldName(inputStream);
        var clans = new PriorityQueue<>(COMPARATOR);
        var clansPerPlayers = new HashMap<Integer, Queue<Clan>>();
        var parsed = parseClans(topLevelField, clans, clansPerPlayers, inputStream);
        if (parsed) {
            topLevelField = parseTopLevelFieldName(inputStream);
        }
        if (topLevelField == TopLevelField.GROUP_COUNT) {
            JsonParser.scrollStreamTill(':', inputStream);
            groupCount = JsonParser.parseInt(inputStream);
        }
        if (!parsed) {
            topLevelField = parseTopLevelFieldName(inputStream);
            parsed = parseClans(topLevelField, clans, clansPerPlayers, inputStream);
        }
        if (!parsed || groupCount == -1) {
            //TODO handle invalid format
            return;
        }
        prepareResponse(clans, clansPerPlayers, groupCount, outputStream);
    }

    private TopLevelField parseTopLevelFieldName(InputStream inputStream) throws IOException {
        JsonParser.scrollStreamTill('"', inputStream);
        return JsonParser.parseFieldName(inputStream, new TopLevelField[TopLevelField.values().length], TopLevelField.values());
    }

    private boolean parseClans(TopLevelField topLevelField, Collection<Clan> clans,
            Map<Integer, Queue<Clan>> clansPerPlayers, InputStream inputStream) throws IOException {
        if (topLevelField != TopLevelField.CLANS) {
            return false;
        }
        parseClans(clans, clansPerPlayers, inputStream);
        return true;
    }

    private void parseClans(Collection<Clan> clans, Map<Integer, Queue<Clan>> clansPerPlayers,
            InputStream inputStream) throws IOException {
        JsonParser.scrollStreamTill('[', inputStream);
        byte ch = -1;
        while (true) {
            while (!clans.isEmpty() && isNotCommaOrBracket((ch = (byte) inputStream.read()))) {}
            if (ch == ']') {
                break;
            }
            var clan = parseClan(inputStream);
            clans.add(clan);
            clansPerPlayers.compute(clan.numberOfPlayers(), (k, v) -> {
                if (v == null) {
                    v = new PriorityQueue<>(COMPARATOR);
                }
                v.add(clan);
                return v;
            });
        }
    }

    private boolean isNotCommaOrBracket(byte c) {
        return c != ',' && c != ']';
    }

    private Clan parseClan(InputStream inputStream) throws IOException {
        var fieldName = parseClanLevelFieldName(inputStream);
        int numberOfPlayers = -1;
        int points = -1;
        if (fieldName == ClanLevelField.NUMBER_OF_PLAYERS) {
            JsonParser.scrollStreamTill(':', inputStream);
            numberOfPlayers = JsonParser.parseInt(inputStream);
            fieldName = parseClanLevelFieldName(inputStream);
        }
        if (fieldName == ClanLevelField.POINTS) {
            JsonParser.scrollStreamTill(':', inputStream);
            points = JsonParser.parseInt(inputStream);
        }
        if (numberOfPlayers == -1) {
            fieldName = parseClanLevelFieldName(inputStream);
            if (fieldName == ClanLevelField.NUMBER_OF_PLAYERS) {
                JsonParser.scrollStreamTill(':', inputStream);
                numberOfPlayers = JsonParser.parseInt(inputStream);
            }
        }
        if (numberOfPlayers == -1 || points == -1) {
            //TODO handle invalid format
            return null;
        }
        return new Clan(numberOfPlayers, points);
    }

    private ClanLevelField parseClanLevelFieldName(InputStream inputStream) throws IOException {
        JsonParser.scrollStreamTill('"', inputStream);
        return JsonParser.parseFieldName(inputStream, new ClanLevelField[ClanLevelField.values().length],
                ClanLevelField.values());
    }

    private void prepareResponse(Queue<Clan> clans, Map<Integer, Queue<Clan>> clansPerPlayers,
            int groupCount, OutputStream outputStream) throws IOException {
        outputStream.write('[');
        int currentGroupCount = groupCount;
        boolean isFirstGroup = true;
        while (!clans.isEmpty()) {
            if (currentGroupCount == groupCount) {
                startGroup(isFirstGroup, outputStream);
            }
            if (clans.peek().numberOfPlayers() <= currentGroupCount) {
                var clan = clans.poll();
                writeClan(clan, currentGroupCount == groupCount, outputStream);
                currentGroupCount -= clan.numberOfPlayers();
                clansPerPlayers.get(clan.numberOfPlayers()).remove(clan);
                if (currentGroupCount == 0) {
                    endGroup(outputStream);
                    currentGroupCount = groupCount;
                    isFirstGroup = false;
                }
            } else {
                var nextNumberOfPlayers = currentGroupCount;
                Queue<Clan> candidates = null;
                while (nextNumberOfPlayers > 0 && isEmpty(candidates = clansPerPlayers.get(nextNumberOfPlayers))) {
                    nextNumberOfPlayers --;
                }
                if (isEmpty(candidates)) {
                    endGroup(outputStream);
                    currentGroupCount = groupCount;
                    isFirstGroup = false;
                    continue;
                }
                var candidate = candidates.poll();
                writeClan(candidate, currentGroupCount == groupCount, outputStream);
                currentGroupCount -= nextNumberOfPlayers;
                clans.remove(candidate);
                if (currentGroupCount == 0) {
                    endGroup(outputStream);
                    currentGroupCount = groupCount;
                    isFirstGroup = false;
                }
            }
        }
        if (currentGroupCount != 0) {
            endGroup(outputStream);
        }
        outputStream.write(']');
    }

    private <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    private void startGroup(boolean isFirstGroup, OutputStream outputStream) throws IOException {
        if (!isFirstGroup) {
            outputStream.write(',');
        }
        outputStream.write('[');
    }

    private void endGroup(OutputStream outputStream) throws IOException {
        outputStream.write(']');
    }

    private void writeClan(Clan clan, boolean isFirstClan, OutputStream outputStream) throws IOException {
        if (!isFirstClan) {
            outputStream.write(',');
        }
        outputStream.write('{');
        JsonParser.writeFieldName("numberOfPlayers", outputStream);
        outputStream.write(':');
        JsonParser.writeInt(clan.numberOfPlayers(), outputStream);
        outputStream.write(',');
        JsonParser.writeFieldName("points", outputStream);
        outputStream.write(':');
        JsonParser.writeInt(clan.points(), outputStream);
        outputStream.write('}');
    }
}
