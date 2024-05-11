/**
	Group Signature Crypto Library
	Copyright (C) 2010 - Diego Ferri
		
	This file is part of Group Signature Crypto Library.

    Group Signature Crypto Library is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Group Signature Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Group Signature Crypto Library.  If not, see <http://www.gnu.org/licenses/>
*/

package it.unibs.gs.test;
public class StopWatch 
{
    private long start;
    private long stop;
    
    public StopWatch()
    {
    	start = 0;
    	stop = -1;
    }
    
    public void start() 
    {
        start = System.currentTimeMillis();
    }
    
    public void stop() 
    {
        stop = System.currentTimeMillis();
    }
    
    public long read() 
    {
        return stop - start;
    }
    
    public String toString() 
    {
    	if (stop < start)
    		return "StopWatch not correctly used.";
    	else
    		return "elapsed Time: " + Long.toString(read()) + "ms";
    }
}
