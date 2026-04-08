/*
 * Copyright (C) 2020 Aibes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package zadscripts;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads database connection settings from db.properties so that credentials
 * and environment-specific values are never hardcoded in source files.
 *
 * @author Aibes
 */
public class DbConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(
                "Missing db.properties — copy db.properties.example and fill in your values", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
