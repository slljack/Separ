#!/bin/bash
java --add-exports java.base/sun.security.jca=ALL-UNNAMED -cp target/classes:target/dependency/* com.separ.$@

