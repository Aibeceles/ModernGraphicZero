/*
 * Copyright (C) 2020 ChiefQuippy
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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 *
 * @author ChiefQuippy
 */
public class ScriptReturnBoolean {

Boolean isEmpty = FALSE;    
    
public ScriptReturnBoolean() {
    
}

public void setBooleanTRUE() {
    isEmpty = TRUE;
    
}

public void setBooleanFALSE(){
    isEmpty = FALSE;
}

public Boolean getBoolean() {
    return(isEmpty.booleanValue());
}

    
}
