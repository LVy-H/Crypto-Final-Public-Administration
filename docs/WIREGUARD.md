# WireGuard Tunnel Configuration

Setup guide for the WireGuard tunnel that exposes the local Kind cluster to a public IP.

## Architecture

```
Internet → Public VPS (20.205.22.180) → WireGuard Tunnel → Kind Cluster → nginx-ingress → Services
```

## VPS Server Setup

On your public VPS (the WireGuard server):

```bash
# Install WireGuard
apt install wireguard

# Generate keys
wg genkey | tee /etc/wireguard/private.key | wg pubkey > /etc/wireguard/public.key

# Create config
cat > /etc/wireguard/wg0.conf << 'EOF'
[Interface]
Address = 10.49.108.1/24
ListenPort = 51820
PrivateKey = <SERVER_PRIVATE_KEY>
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

[Peer]
# Kind cluster peer
PublicKey = <KIND_CLUSTER_PUBLIC_KEY>
AllowedIPs = 10.49.108.2/32
EOF

# Enable and start
systemctl enable wg-quick@wg0
systemctl start wg-quick@wg0
```

## Kind Cluster Setup

### 1. Create WireGuard Secret

Edit `infra/k8s/base/wg-secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: wireguard-config
  namespace: ingress-nginx
type: Opaque
stringData:
  wg0.conf: |
    [Interface]
    Address = 10.49.108.2/24
    PrivateKey = <KIND_CLUSTER_PRIVATE_KEY>

    [Peer]
    PublicKey = <VPS_SERVER_PUBLIC_KEY>
    Endpoint = 20.205.22.180:51820
    AllowedIPs = 10.49.108.0/24
    PersistentKeepalive = 25
```

### 2. Deploy WireGuard Proxy

```bash
kubectl apply -f infra/k8s/base/wg-secret.yaml
kubectl apply -f infra/k8s/base/wg-proxy.yaml
```

### 3. Verify Connection

```bash
# Check WireGuard status
kubectl exec -n ingress-nginx deploy/wg-proxy -c wireguard -- wg show

# Should show:
# peer: <VPS_PUBLIC_KEY>
#   endpoint: 20.205.22.180:51820
#   latest handshake: X seconds ago
```

## DNS Configuration

Point your domain to the VPS public IP:

```
portal.gov-id.lvh.id.vn  →  20.205.22.180
api.gov-id.lvh.id.vn     →  20.205.22.180
```

> **Note:** `lvh.id.vn` resolves to 127.0.0.1 by default. For local testing, use port-forward instead.

## Traffic Flow

1. Request hits VPS:51820 via WireGuard
2. VPS forwards to Kind cluster via tunnel (10.49.108.2)
3. wg-proxy DNAT rules forward to nginx-ingress pod
4. nginx-ingress routes to appropriate service based on Host header

## Troubleshooting

### No handshake
```bash
# Check firewall on VPS
ufw allow 51820/udp

# Check WireGuard logs
kubectl logs deploy/wg-proxy -n ingress-nginx -c wireguard
```

### DNAT not working
```bash
# Check iptables rules
kubectl exec -n ingress-nginx deploy/wg-proxy -c dnat-manager -- iptables -t nat -L PREROUTING -n

# Check nginx-ingress pod IP
kubectl get pods -n ingress-nginx -l app=nginx-pqc-ingress -o wide
```

### Restart tunnel
```bash
kubectl rollout restart deployment/wg-proxy -n ingress-nginx
```
