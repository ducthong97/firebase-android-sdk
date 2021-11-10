// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.crashlytics.internal.persistence;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.firebase.crashlytics.internal.Logger;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Controls creation files that are used by Crashlytics.
 *
 * The following types of files are used by Crashlytics:
 * <ul>
 * <li>"Common" files, that exist independent of
 * a specific session id. </li>
 * <li> "Open session" files, which contain a varierty of temporary files specific ot a Crashlytics
 * session. These files may or may not eventually be combined into a Crashlytics crash report file. </li>
 * <li> "Report" files, which are processed reports, ready to be uploaded to Crashlytics servers. </li>
 * </ul>
 * The distinction allows us to put intermediate session files in session-specific directories, so
 * that all files for a session can be easily cleaned up by deleting that subdirectory. It also
 * allows us to grab all prepared reports quickly, etc.
 *
 * The files are stored in an versioned Crashlytics-specific directory, which is versioned to make
 * it straightforward to change how / where we store files in the future.
 *
 * The current structure is:
 * <code>
 *   .com.google.firebase.crashlytics.files.v1/
 *     open-sessions/
 *       SESSION-ID-A/
 *         file1
 *         file2
 *         ...
 *       SESSION-ID-B/
 *         file1
 *         file2
 *         ...
 *       SESSION-ID-n/
 *         ...
 *     prepared-reports/
 *       SESSION-ID-A.priority
 *       SESSION-ID-B
 *       SESSION-ID-C.native
 *       ...
 * </code>
 *
 * By convention, any use of new File(...) or similar outside of this class is a code smell.
 */
public class FileStore {

  private static final String FILES_PATH = ".com.google.firebase.crashlytics.files.v1";
  private static final String SESSIONS_PATH = "open-sessions";
  private static final String PREPARED_REPORTS_PATH = "prepared-reports";

  public static final String PRIORITY_REPORT_EXTENSION = ".priority";
  public static final String NATIVE_REPORT_EXTENSION = ".native";

  private final File rootDir;
  private final File sessionsDir;
  private final File reportsDir;

  public FileStore(Context context) {
    rootDir = prepareDir(new File(context.getFilesDir(), FILES_PATH));
    sessionsDir = prepareDir(new File(rootDir, SESSIONS_PATH));
    reportsDir = prepareDir(new File(rootDir, PREPARED_REPORTS_PATH));
  }

  public void cleanupLegacyFiles() {
    File legacyDir = new File(rootDir.getParent(), ".com.google.firebase.crashlytics");
    if (recursiveDelete(legacyDir)) {
      Logger.getLogger().d("Deleted legacy Crashlytics files from " + legacyDir.getPath());
    }
  }

  static public boolean recursiveDelete(File fileOrDirectory) {
    File[] files = fileOrDirectory.listFiles();
    if (files != null) {
      for (File file : files) {
        recursiveDelete(file);
      }
    }
    return fileOrDirectory.delete();
  }


  /** @return internal File used by Crashlytics, that is not specific to a session */
  public File getCommonFile(String filename) {
    return new File(prepareDir(rootDir), filename);
  }

  /** @return all common (non session specific) files matching the given filter. */
  public List<File> getCommonFiles(FilenameFilter filter) {
    return nullableArrayToList(rootDir.listFiles(filter));
  }

  /**
   * @return A file with the given filename, in the session-specific directory corresponding to the
   *     given session Id.
   */
  public File getSessionFile(String sessionId, String filename) {
    return new File(new File(sessionsDir, sessionId), filename);
  }

  public List<File> getSessionFiles(String sessionId, FilenameFilter filter) {
    return nullableArrayToList((new File(sessionsDir, sessionId)).listFiles(filter));
  }

  public boolean deleteSessionFiles(String sessionId) {
    File sessionDir = new File(sessionsDir, sessionId);
    return recursiveDelete(sessionDir);
  }

  public List<String> getAllOpenSessionIds() {
    return nullableArrayToList(sessionsDir.list());
  }

  public File getReportFile(String sessionId) {
    return new File(reportsDir, sessionId);
  }

  public File getPriorityReportFile(String sessionId) {
    return getReportFile(sessionId + PRIORITY_REPORT_EXTENSION);

  }

  public File getNativeReportFile(String sessionId) {
    return getReportFile(sessionId + NATIVE_REPORT_EXTENSION);
  }

  public List<File> getAllReportFiles() {
    return nullableArrayToList(reportsDir.listFiles());
  }

  public boolean deleteReport(String sessionId) {
    // :TODO HW2021: Make this just the one-liner? return getReportFile(sessionId).delete()
    File reportFile = getReportFile(sessionId);
    if (reportFile.exists()) {
      Logger.getLogger().v("Deleting session report: " + reportFile.getPath());
      return reportFile.delete();
    }
    Logger.getLogger().d("Could not find report file to delete: " + reportFile.getPath());
    return false;
  }

  static synchronized File prepareDir(File file) {
    if (file.exists()) {
      if (file.isDirectory()) {
        return file;
      } else {
        Logger.getLogger()
            .d(
                "Unexpected non-directory file: "
                    + file
                    + "; deleting file and creating new directory.");
        file.delete();
      }
    }
    if (!file.mkdirs()) {
      throw new IllegalStateException("Could not create Crashlytics-specific directory: " + file);
    }
    return file;
  }

  private static <T> List<T> nullableArrayToList(@Nullable T[] array) {
    return (array == null) ? Collections.emptyList() : Arrays.asList(array);
  }
}
