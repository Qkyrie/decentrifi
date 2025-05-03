# Build landing page into nginx image
FROM nginx:alpine

# Copy all landing files to the nginx directory
COPY . /usr/share/nginx/html

# Expose port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
