# ds-replicated-log
## Assumptions:
The logger-replication cluster consists of a master node and 2 secondaries.

Hosts are predefined in docker-compose.yml:
- master:6565
- secondary-1:6767
- secondary-2:6767

## Run the Cluster
docker-compose up --build


