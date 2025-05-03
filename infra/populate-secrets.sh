kubectl create secret generic decentrifi-secrets \
  --from-env-file=secrets.env \
  --namespace=decentrifi \
  --dry-run=client -o yaml \
| kubectl apply -f -