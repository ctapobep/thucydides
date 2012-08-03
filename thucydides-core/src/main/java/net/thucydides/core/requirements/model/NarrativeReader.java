package net.thucydides.core.requirements.model;

import com.google.common.base.Optional;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.issues.IssueTracking;
import net.thucydides.core.reports.html.Formatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import static ch.lambdaj.Lambda.index;
import static ch.lambdaj.Lambda.sort;
import static java.util.Collections.reverseOrder;

/**
 * Load a narrative text from a directory.
 * A narrative is a text file that describes a capability, feature, or epic, or whatever terms you are using in your
 * project. The directory structure itself is used to organize capabilities into features, and so on. At the leaf
 * level, the directory will contain story files (e.g. JBehave stories, JUnit test cases, etc). At each level, a
 * "*.narrative" file provides a description.
 *
 */
public class NarrativeReader {
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String TITLE_SEPARATOR = ":";
    private final IssueTracking issueTracking = Injectors.getInjector().getInstance(IssueTracking.class);

    public Optional<Narrative> loadFrom(File directory) {
        File[] narrativeFiles = directory.listFiles(endingInNarrative());
        if (narrativeFiles.length == 0) {
            return Optional.absent();
        } else {
            return Optional.fromNullable(narrativeLoadedFrom(narrativeFiles[0]));
        }
    }

    private Narrative narrativeLoadedFrom(File narrativeFile) {
        Narrative narrative = null;
        String type = withoutFileExtension(narrativeFile.getName());
        try {

            BufferedReader reader = new BufferedReader(new FileReader(narrativeFile));

            String title = null;
            String cardNumber = null;
            Optional<String> titleLine = readOptionalTitleLine(reader);
            if (titleLine.isPresent()) {
                title = readTitleFrom(titleLine.get());
                cardNumber = findCardNumberIn(titleLine.get());
            }
            String text = readDescriptionFrom(reader);
            reader.close();
            narrative = new Narrative(Optional.fromNullable(title), Optional.fromNullable(cardNumber), type, text);
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        return narrative;
    }

    private Optional<String> readOptionalTitleLine(BufferedReader reader) throws IOException {
        String titleLine = reader.readLine();
        if (titleLine.contains(TITLE_SEPARATOR)) {
            return Optional.of(titleLine);
        } else {
            return Optional.absent();
        }
    }

    private String findCardNumberIn(String titleLine) {
        List<String> issues = Formatter.issuesIn(titleLine);
        if (!issues.isEmpty()) {
            return issues.get(0);
        } else {
            return null;
        }
    }


    private String withoutFileExtension(String name) {
        int fileExtensionIndex = name.indexOf(".");
        return name.substring(0, fileExtensionIndex);
    }

    private String readTitleFrom(String titleLine) throws IOException {
        int separatorAt = titleLine.indexOf(TITLE_SEPARATOR);
        if (separatorAt > 0) {
            return titleLine.substring(separatorAt + 1).trim();
        } else {
            return null;
        }
    }

    private String readDescriptionFrom(BufferedReader reader) throws IOException {
        StringBuilder description = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            description.append(line);
            description.append(NEW_LINE);
        }
        return description.toString();
    }

    private FilenameFilter endingInNarrative() {
        return new FilenameFilter() {

            @Override
            public boolean accept(File file, String name) {
                return name.toLowerCase().endsWith(".narrative");
            }
        };
    }
}