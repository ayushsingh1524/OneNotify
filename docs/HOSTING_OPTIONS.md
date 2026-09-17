# Hosting options — checked 17 September 2026

Recommendation: evaluate **DigitalOcean in Bangalore** for the first synthetic-data staging environment. The prepared Docker Compose stack fits a single Linux VM. My recommendation is based on deployment simplicity and a nearby region, not a benchmark or a compliance determination.

| Option | Published compute price | Fit and tradeoff |
|---|---|---|
| DigitalOcean Basic Droplet | Regular 8 GiB / 4 vCPU: US$48/month; 16 GiB / 8 vCPU: US$96/month | Straightforward VM setup; Bangalore region is listed. You still operate updates, backups, database and monitoring. |
| AWS Lightsail | Linux public-IPv4 8 GB / 2 vCPU: US$44/month; 16 GB / 4 vCPU: US$84/month | Alternative for an AWS account and Mumbai hosting. More AWS services can be adopted later. Confirm bundle availability and transfer allowance for Mumbai. |
| Hetzner Cloud | Obtain current Singapore plan quote from console | Worth comparing if Singapore is acceptable. June 2026 price changes make older low-price recommendations unreliable. |

Sources: [DigitalOcean pricing](https://www.digitalocean.com/pricing/droplets), [DigitalOcean regions](https://docs.digitalocean.com/platform/regional-availability/), [Lightsail pricing](https://aws.amazon.com/lightsail/pricing/), [Hetzner locations](https://docs.hetzner.com/cloud/general/locations/), [Hetzner price changes](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/).

Prices exclude applicable tax, domain registration, email, independent backup storage, extra traffic and review/support costs. They are reference prices, not a purchase quote. Check the selected region at checkout. No account or paid resource was created.

ClamAV documentation recommends 4 GB for its container. That makes an 8 GB VM an initial staging candidate, with 16 GB safer for concurrent application builds and scanner updates. Measure actual memory/CPU before choosing a production size. A small free-tier frontend host alone cannot run this entire PostgreSQL/Kafka/MinIO/Java stack. [ClamAV resource guidance](https://docs.clamav.net/manual/Installing/Docker.html)

## What you will eventually choose

1. Hosting account and spending limit.
2. Domain/registrar and a staging subdomain.
3. Transactional SMTP provider and verified sender domain.
4. Who operates the server and receives alerts.
5. Required deployment region, after privacy/data-processing review.

For now, continue locally and review the prepared staging files. There is no reason to buy a server before the walkthrough and configuration review are satisfactory.
