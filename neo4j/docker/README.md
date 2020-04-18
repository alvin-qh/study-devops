# Neo4j Docker

## Trouble shooting

### Change promission of mounted volumes

1. Startup docker container;
2. Genrate `import`, `logs`, `plugins` folders automate as docker volumes;
3. Change promissions of those folders;

```bash
$ sudo chmod -R 755 import logs plugins
```