This is the extended version of the Separ.

This code can be ran with the same command.

commands-byzantine.
./run.sh RegistrationAuthority
./run.sh entity.Requester 0
./run.sh entity.Worker 0 3
------ platform -------------
./run.sh entity.PlatformWithNodes 3 12 0 2
----- or ----------
./run.sh entity.Platform 3 12 0 2
./run.sh entity.Node 0 0 3
./run.sh entity.Node 1 4 7
./run.sh entity.Node 2 8 11


commands-crashonly

./run.sh RegistrationAuthority
./run.sh entity.Requester 0
./run.sh entity.Worker 0 3
------ platform -------------
./run.sh entity.PlatformWithNodes 4 12 0 3
----- or ----------
./run.sh entity.Platform 4 12 0 3
./run.sh entity.Node 0 0 2
./run.sh entity.Node 1 3 5
./run.sh entity.Node 2 6 8
./run.sh entity.Node 3 9 11
