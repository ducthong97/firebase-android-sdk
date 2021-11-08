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
import com.google.firebase.crashlytics.internal.Logger;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileStore {

  private static final String FILES_PATH = ".com.google.firebase.crashlytics";
  private static final String SESSIONS_PATH = "sessions";
  private static final String PREPARED_REPORTS_PATH = "prepared-reports";

  private final File rootDir;
  private final File sessionsDir;
  private final File reportsDir;

  public FileStore(Context context) {
    rootDir = prepareDir(new File(context.getFilesDir(), FILES_PATH));
    sessionsDir = prepareDir(new File(rootDir, SESSIONS_PATH));
    reportsDir = prepareDir(new File(rootDir, PREPARED_REPORTS_PATH));
  }

  /** @return internal File used by Crashlytics, that is not specific to a session */
  public File getCommonFile(String filename) {
    return new File(prepareDir(rootDir), filename);
  }

  public List<File> getSessionDirs() {
    return null; // :TODO:
  }

  /**
   * @return A file with the given filename, in the session-specific directory corresponding to the
   *     given session Id.
   */
  public File getSessionFile(String sessionId, String filename) {
    return new File(new File(sessionsDir, sessionId), filename);
  }

  public List<File> getReports() {
    return Arrays.asList(reportsDir.listFiles());
  }

  public File getReportFile(String sessionId) {
    return new File(reportsDir, sessionId);
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
}
