/*
 * Copyright Tero Saarni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package keystore_tutorial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code KeyStoreSpi} implementation that hot-reloads {@code KeyStore} when the backing file changes.
 */
public class ReloadingKeyStoreFileSpi extends DelegatingKeyStoreSpi {

  private static final Logger log = LoggerFactory.getLogger(ReloadingKeyStoreFileSpi.class);

  private final String type;
  private final String provider;
  private final Path path;
  private final char[] password;
  private FileTime lastModified;

  public ReloadingKeyStoreFileSpi(String type, String provider, Path path, String password) throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException, IOException {
    if (password == null) {
      throw new IllegalArgumentException("Password must not be null");
    }

    this.type = type;
    this.provider = provider;
    this.path = path;
    this.password = password.toCharArray();

    refresh();
  }

  /**
   * Reload keystore if it was modified on disk since it was last loaded.
   * @throws IOException
   * @throws NoSuchProviderException
   * @throws KeyStoreException
   * @throws CertificateException
   * @throws NoSuchAlgorithmException
   */
  void refresh() throws IOException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException {
    // If keystore has been previously loaded, check the modification timestamp to decide if reload is needed.
    if ((lastModified != null) && (lastModified.compareTo(Files.getLastModifiedTime(path)) > 0)) {
      // File was not modified since last reload: do nothing.
      return;
    }

    // Load keystore from disk.
    log.debug("Reloading keystore {}", path);
    KeyStore ks = KeyStore.getInstance(type, provider);
    ks.load(Files.newInputStream(path), password);
    setKeyStoreDelegate(ks);
    this.lastModified = Files.getLastModifiedTime(path);
  }

}
