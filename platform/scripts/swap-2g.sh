#!/bin/bash
# Crea y activa /swapfile de 2 GB en la VM si no existe. Red de seguridad, no memoria de trabajo.
set -euo pipefail

SWAPFILE=/swapfile

if swapon --show=NAME --noheadings | grep -qx "$SWAPFILE"; then
  echo "swap ya activo en $SWAPFILE"
  free -m
  exit 0
fi

if [ ! -f "$SWAPFILE" ]; then
  sudo fallocate -l 2G "$SWAPFILE" || sudo dd if=/dev/zero of="$SWAPFILE" bs=1M count=2048
  sudo chmod 600 "$SWAPFILE"
  sudo mkswap "$SWAPFILE"
fi

sudo swapon "$SWAPFILE"
grep -q "^$SWAPFILE " /etc/fstab || echo "$SWAPFILE none swap sw 0 0" | sudo tee -a /etc/fstab > /dev/null
sudo sysctl -q vm.swappiness=10
grep -q '^vm.swappiness' /etc/sysctl.conf || echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf > /dev/null
free -m
