install plugin group_replication soname 'group_replication.so';

set global group_replication_group_name = '38f34157-cbe8-4623-a7bd-054cc5c2de0b';
set global group_replication_start_on_boot = off;
set global group_replication_local_address = 'mgr_01:10061';
set global group_replication_group_seeds ='mgr_01:10061,mgr_02:10061,mgr_03:10061';
set global group_replication_bootstrap_group = off;

set global group_replication_single_primary_mode=on;
set global group_replication_bootstrap_group=ON;

CHANGE MASTER TO MASTER_USER='repl', MASTER_PASSWORD='repl' FOR CHANNEL 'group_replication_recovery';

RESET MASTER;

start group_replication;
set global group_replication_bootstrap_group=OFF;






install plugin group_replication soname 'group_replication.so';

set global group_replication_group_name = '38f34157-cbe8-4623-a7bd-054cc5c2de0b';
set global group_replication_start_on_boot = off;
set global group_replication_local_address = 'mgr_02:10061';
set global group_replication_group_seeds ='mgr_01:10061,mgr_02:10061,mgr_03:10061';
set global group_replication_bootstrap_group = off;

CHANGE MASTER TO MASTER_USER='repl', MASTER_PASSWORD='repl' FOR CHANNEL 'group_replication_recovery';

RESET MASTER;

start group_replication;



install plugin group_replication soname 'group_replication.so';

set global group_replication_group_name = '38f34157-cbe8-4623-a7bd-054cc5c2de0b';
set global group_replication_start_on_boot = off;
set global group_replication_local_address = 'mgr_03:10061';
set global group_replication_group_seeds ='mgr_01:10061,mgr_02:10061,mgr_03:10061';
set global group_replication_bootstrap_group = off;

CHANGE MASTER TO MASTER_USER='repl', MASTER_PASSWORD='repl' FOR CHANNEL 'group_replication_recovery';

RESET MASTER;

start group_replication;





select * from performance_schema.replication_group_members;



stop group_replication;
set global group_replication_single_primary_mode=OFF;
set global group_replication_enforce_update_everywhere_checks=ON;

SET GLOBAL group_replication_bootstrap_group=ON;
START GROUP_REPLICATION;
SET GLOBAL group_replication_bootstrap_group=OFF;



START GROUP_REPLICATION;
