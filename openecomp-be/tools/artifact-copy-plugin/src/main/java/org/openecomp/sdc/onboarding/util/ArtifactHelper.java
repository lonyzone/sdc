/*
 * Copyright © 2018 European Support Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on a "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openecomp.sdc.onboarding.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public class ArtifactHelper {

    private MavenProject project;
    private MavenSession session;
    private static final Map<String, byte[]> store = new HashMap<>();
    private static final Set<String> terminalModuleCoordinates = new HashSet<>();
    private String unicornRoot = null;
    private static File unicornMetaLocation = null;
    private final File tempLocation = Paths.get(System.getProperties().getProperty("java.io.tmpdir")).toFile();
    private static int snapshotBuildNumber = 0;
    private static final String HYPHEN = "-";

    void init(String terminalModuleCoordinate) {
        setUnicornMetaLocation(getUnicornRootFile(unicornRoot.substring(0, unicornRoot.indexOf('/')), project));
        setTerminalModuleCoordinates(session.getProjects().get(session.getProjects().size() - 1));
        terminalModuleCoordinates.add(terminalModuleCoordinate);
    }

    private static void setUnicornMetaLocation(File file) {
        unicornMetaLocation = file;
    }

    static void setSnapshotBuildNumber(int number) {
        snapshotBuildNumber = number;
    }

    List<ArtifactRepository> getRepositories(boolean snapshotRepo) {
        List<ArtifactRepository> list = new ArrayList<>();
        for (ArtifactRepository artRepo : project.getRemoteArtifactRepositories()) {
            if (snapshotRepo) {
                if (artRepo.getSnapshots().isEnabled()) {
                    list.add(artRepo);
                }
            } else {
                if (artRepo.getReleases().isEnabled()) {
                    list.add(artRepo);
                }
            }
        }
        return list;
    }

    private void setTerminalModuleCoordinates(MavenProject project) {
        terminalModuleCoordinates.add(getModuleCoordinate(project));
    }

    private boolean isModuleTerminal(MavenProject project) {
        return terminalModuleCoordinates.contains(getModuleCoordinate(project));
    }

    File getUnicornMetaLocation() {
        return unicornMetaLocation;
    }

    String getContents(URL path) throws IOException {
        try (InputStream is = path.openStream(); Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    void store(String artifactId, byte[] data) {
        store.put(artifactId, data);
    }

    void deleteAll(File f) {

        if (!f.exists() || !f.isDirectory()) {
            return;
        }

        File[] fileList = f.listFiles();
        if (fileList == null) {
            return;
        }

        for (File file : fileList) {

            if (file.isFile()) {
                file.delete();
            }
        }
    }

    String getModuleCoordinate(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId();
    }

    private File getUnicornRootFile(String moduleCoordinate, MavenProject mavenProject) {
        return getStateFile(moduleCoordinate, mavenProject, unicornRoot);
    }

    private File getStateFile(String moduleCoordinate, MavenProject mavenProject, String filePath) {
        return new File(getTopParentProject(moduleCoordinate, mavenProject).getBasedir(),
                filePath.substring(filePath.indexOf('/') + 1));
    }

    MavenProject getTopParentProject(String moduleCoordinate, MavenProject mavenProject) {
        if (getModuleCoordinate(mavenProject).equals(moduleCoordinate) || mavenProject.getParent() == null) {
            return mavenProject;
        } else {
            return getTopParentProject(moduleCoordinate, mavenProject.getParent());
        }
    }

    void shutDown(MavenProject project) throws IOException, ClassNotFoundException {
        File file = new File(unicornMetaLocation, "compileState.dat");
        HashMap dataStore;
        if (isModuleTerminal(project) && file.exists()) {
            try (InputStream is = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(is)) {
                dataStore = (HashMap) ois.readObject();
                //noinspection unchecked
                dataStore.put("shutdownTime", (System.currentTimeMillis() / 1000) * 1000 + snapshotBuildNumber);
                //noinspection unchecked
                dataStore.put("version", project.getVersion());
            }
            try (OutputStream os = new FileOutputStream(file); ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeObject(dataStore);
            }
            Files.copy(file.toPath(),
                    Paths.get(tempLocation.getAbsolutePath(), file.getName() + HYPHEN + project.getVersion()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}


